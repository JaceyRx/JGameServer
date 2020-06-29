package com.jacey.game.gateway.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.NetResponseMessage;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.SessionIdService;
import com.jacey.game.gateway.manager.MessageManager;
import com.jacey.game.gateway.manager.OnlineClientManager;
import com.jacey.game.gateway.manager.SpringManager;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @Description: 每一个Channel都需要绑定一个Actor(做线程隔离)
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class ChannelActor extends UntypedAbstractActor {

    private static final AttributeKey<ActorRef> NETTY_CHANNEL_TO_ACTOR_KEY = AttributeKey
            .valueOf("nettyChannelToActorKey");
    private static final AttributeKey<Integer> NETTY_CHANNEL_TO_SESSION_ID_KEY = AttributeKey
            .valueOf("nettyChannelToSessionIdKey");

    /** 当前ChannelActor说绑定的Channel */
    private Channel channel;
    /** 当前连接的 UserId */
    private int userId;
    /** 当前连接的ip */
    private String userIp;

    /** 当前ChannelActor绑定的ResponseActor.用于接收远程服务器的响应 */
    private ActorRef responseActor;

    private BattleServerLoadBalanceService battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceService.class);
    private static SessionIdService sessionIdService = SpringManager.getInstance().getBean(SessionIdService.class);

    public ChannelActor(Channel channel) {
        this.channel = channel;
        InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.remoteAddress();
        userIp = inetSocketAddress.getAddress().getHostAddress();
    }

    /**
     * Actor 启动时调用
     * 每个ChannelActor绑定一个ResponseActor用于接收远程服务器返回的消息
     * @throws Exception
     */
    @Override
    public void preStart() throws Exception {
        // 每个ChannelActor都需要创建一个附属的ResponseActor用于接收远程服务器返回的消息
        // 因为客户端向gateway发送的NetMessage在这里处理，而远程服务器收到gateway转发的NetMessage，处理后也是以NetMessage形式回复的
        // 如果都在ChannelActor中处理，因为都是NetMessage就无法区分
        responseActor = context().actorOf(Props.create(ResponseActor.class, channel));
    }

    /**
     * Actor 关闭是调用。@客户端断线处理
     * @throws Exception
     */
    @Override
    public void postStop() throws Exception {
        int sessionId = getSessionId(channel);
        OnlineClientManager.getInstance().removeSession(sessionId);
    }

    /**
     * Actor接收到消息是调用
     * @param o
     * @throws Throwable
     */
    @Override
    public void onReceive(Object o) throws Throwable {
        if (o instanceof NetMessage) {
            NetMessage msg = (NetMessage) o;
            int sessionId = getSessionId(channel);
            msg.setUserId(userId);
            msg.setSessionId(sessionId);
            switch (msg.getRpcNum()) {
                case Rpc.RpcNameEnum.Regist_VALUE: {
                    /** 注册协议 */
                    if (msg.getUserId() > 0) {
                        log.error("【注册异常】 已登录用户，不能重复请求注册, userId = {}", msg.getUserId());
                        sendErrorToClient(msg, Rpc.RpcErrorCodeEnum.ServerError_VALUE);
                        break;
                    }
                    msg.setUserIp(userIp);
                    // 注册消息发送到主逻辑服务器处理
                    if (MessageManager.getInstance().sendNetMsgToMainLogicServer(msg, responseActor) == false) {
                        // 发送失败：将错误信息返回Client
                        sendErrorToClient(msg, Rpc.RpcErrorCodeEnum.ServerNotAvailable_VALUE);
                    }
                    break;
                }
                case Rpc.RpcNameEnum.Login_VALUE: {
                    /** 登录协议 */
                    if (msg.getUserId() > 0) {
                        log.error("【登录异常】 已登录用户，不能重复请求登录, userId = {}", msg.getUserId());
                        sendErrorToClient(msg, Rpc.RpcErrorCodeEnum.ServerError_VALUE);
                        break;
                    }
                    msg.setUserIp(userIp);
                    // 推送消息到该SessionId指定的Logic服务器处理
                    // 如果没有则转发到空闲的logic服务处理
                    if (MessageManager.getInstance().sendNetMsgToLogicServer(msg, responseActor) == false) {
                        // 发送失败：将错误信息返回Client
                        sendErrorToClient(msg, Rpc.RpcErrorCodeEnum.ServerNotAvailable_VALUE);
                    }
                    break;
                }
                case Rpc.RpcNameEnum.Match_VALUE:
                case Rpc.RpcNameEnum.CancelMatch_VALUE:{
                    // 匹配与取消匹配协议消息转发
                    // 只有登录成功，才能发起该操作
                    if (userId > 0) {
                        // 通过akka actor remote path远程调用主逻辑服务器，转发消息
                        if (MessageManager.getInstance().sendNetMsgToMainLogicServer(msg, responseActor) == false) {
                            // 发送失败：将错误信息返回Client
                            sendErrorToClient(msg, Rpc.RpcErrorCodeEnum.ServerNotAvailable_VALUE);
                        }
                    } else {
                        // 如果没有登录，则关闭该会话
                        channel.close();
                    }
                    break;
                }
                // 转发到battleServer的请求
                case Rpc.RpcNameEnum.GetBattleInfo_VALUE:				// 对战相关
                case Rpc.RpcNameEnum.Concede_VALUE:						// 投降认输
                case Rpc.RpcNameEnum.PlacePieces_VALUE:					// 落子
                case Rpc.RpcNameEnum.ReadyToStartGame_VALUE: {		    // 确认可以开始游戏、超时未确认可以开始游戏，则强制开始游戏
                    // 只有登录成功，才能发起该操作
                    if (userId > 0) {
                        // 获取userId对应的battleId(用于判断该用户是否在对战中)
                        String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
                        // 只有在对战中才转发对战信息
                        if (battleId != null) {
                            if (MessageManager.getInstance().sendNetMsgToBattleServer(msg, responseActor) == false) {
                                sendErrorToClient(msg, Rpc.RpcErrorCodeEnum.ServerNotAvailable_VALUE);
                            }
                        } else {
                            sendErrorToClient(msg, Rpc.RpcErrorCodeEnum.UserNotInBattle_VALUE);
                        }
                    } else {
                        channel.close();
                    }
                    break;
                }
                case Rpc.RpcNameEnum.BattleChatText_VALUE:      // 推送聊天文本
                case Rpc.RpcNameEnum.JoinChatRoom_VALUE: {      // 加入聊天房间
                    /** 聊天请求处理 */
                    // 判断是否已经登录
                   if (userId > 0) {
                       // 获取userId对应的battleId(用于判断该用户是否在对战中)
                       String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
                       // 只有在对战中才转发对战信息
                       if (battleId != null) {
                           if (MessageManager.getInstance().sendNetMsgToChatServer(msg, responseActor) == false) {
                               sendErrorToClient(msg, Rpc.RpcErrorCodeEnum.ServerNotAvailable_VALUE);
                           }
                       } else {
                           sendErrorToClient(msg, Rpc.RpcErrorCodeEnum.BattleChatTextErrorNotJoinBattle_VALUE);
                       }
                   }
                    break;
                }
                default: {
                    log.error("【netMessage解析异常】 not support netResponseMessage type = {}",
                            o.getClass().getName());
                    break;
                }
            }
        } else if (o instanceof NetResponseMessage) {
            NetResponseMessage netResponseMessage = (NetResponseMessage) o;
            NetMessage msg = netResponseMessage.getNetMessage();
            int errorCode = msg.getErrorCode();
            switch (msg.getRpcNum()) {
                case Rpc.RpcNameEnum.Login_VALUE: {
                    /** 登录响应 */
                    if (errorCode == Rpc.RpcErrorCodeEnum.Ok_VALUE) {
                        // 登录成功后绑定userId 用于权鉴
                        userId = msg.getUserId();
                    }
                    write(msg);
                    break;
                }
                default: {
                    log.error("【netResponseMessage 消息解析异常】 not support netResponseMessage type = {}",
                            o.getClass().getName());
                    break;
                }
            }
        } else {
            log.error("【消息解析失败】, not support message type = {}", o.getClass().getName());
        }
    }

    /**
     * 获取Chann存储的SessionId
     * @param channel
     * @return
     */
    public static int getSessionId(Channel channel) {
        Attribute<Integer> sessionIdAttr = channel.attr(NETTY_CHANNEL_TO_SESSION_ID_KEY);
        return sessionIdAttr.get();
    }

    /**
     * 创建或获取Channel存储的ActorRef对象
     * @param channel
     * @return ChannelActor
     */
    public static ActorRef attachChannelActor(Channel channel) {
        // 获取channel 的key为：nettyChannelToActorKey的att属性数据
        Attribute<ActorRef> actorAttr = channel.attr(NETTY_CHANNEL_TO_ACTOR_KEY);
        ActorRef actor = actorAttr.get();
        /** 为null说明是重新连接 */
        if (actor == null) {
            // 自动生成session
            int sessionId = sessionIdService.addAndGetNextAvailableSessionId();
            // 获取channel存储sessionId的att属性数据
            Attribute<Integer> sessionIdAttr = channel.attr(NETTY_CHANNEL_TO_SESSION_ID_KEY);
            sessionIdAttr.setIfAbsent(sessionId);
            // 创建新ChannelActor对象与channel绑定
            ActorRef newActor = MessageManager.getInstance().createChannelActor(channel);
            // 注意：只有当attr中已经存在对应值时直接返回值，否则返回null。所以后面需要判断actor是否为空并赋值
            actor = actorAttr.setIfAbsent(newActor);
            if (actor == null) {
                actor = newActor;
            }
            // 用于SessionId与channel、GatewayId绑定。并更新服务器负载
            OnlineClientManager.getInstance().addSession(sessionId, channel);
        }
        return actor;
    }

    /**
     * 推送错误信息给客户端
     * @param netMessage
     * @param errorCode
     */
    private void sendErrorToClient(NetMessage netMessage, int errorCode) {
        NetMessage resp = new NetMessage(netMessage.getRpcNum(), errorCode);
        sender().tell(resp, ActorRef.noSender());
    }

    private void write(IMessage msg) {
        if (this.channel != null && this.channel.isActive() && this.channel.isWritable()) {
            channel.writeAndFlush(msg);
        }
    }

    /**
     * 获取Channel存储的ChannelActor对象
     * @param channel
     * @return
     */
    public static ActorRef getChannelActor(Channel channel) {
        Attribute<ActorRef> actorAttr = channel.attr(NETTY_CHANNEL_TO_ACTOR_KEY);
        return actorAttr.get();
    }

}
