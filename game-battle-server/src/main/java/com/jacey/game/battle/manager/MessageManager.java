package com.jacey.game.battle.manager;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.jacey.game.battle.actor.BaseMessageActor;
import com.jacey.game.battle.actor.BattleRoomManagerActor;
import com.jacey.game.common.action.BaseMessageAction;
import com.jacey.game.common.constants.GlobalConstant;
import com.jacey.game.common.manager.IManager;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.battle.actor.BattleServerActor;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.service.ChatServerLoadBalanceService;
import com.jacey.game.db.service.SessionIdService;
import com.jacey.game.db.service.impl.ChatServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.SessionIdServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class MessageManager implements IManager {

    private MessageManager(){}

    private static MessageManager instance = new MessageManager();

    public static MessageManager getInstance() {
        return instance;
    }

    private SessionIdService sessionIdService;
    private ChatServerLoadBalanceService chatServerLoadBalanceService;



    /** battleServer System Actor */
    private ActorSystem system;
    /** battleServer User Actor */
    private ActorRef battleServerActor;
    /** GM Remote Actor */
    private ActorSelection gmRemoteActor;
    // 是否连接上GM服务器
    public boolean isConnectToGm = false;

    // key:rpcNum, value:对应处理这个消息的Actor
    private final Map<Integer, ActorRef> rpcNumToHandleActorMap = new HashMap<Integer, ActorRef>();
    // key:actor, value:map(key:rpcNum, value:处理这个消息的Action)
    private final Map<Class<? extends BaseMessageActor>, Map<Integer, Class<BaseMessageAction>>> actorToHandleActionMap = new HashMap<Class<? extends BaseMessageActor>, Map<Integer, Class<BaseMessageAction>>>();


    @Override
    public void init() {
        sessionIdService = SpringManager.getInstance().getBean(SessionIdServiceImpl.class);
        chatServerLoadBalanceService = SpringManager.getInstance().getBean(ChatServerLoadBalanceServiceImpl.class);

        // ActorSystem name [battleServer_ + battleServerId]
        system = ActorSystem.create(GlobalConstant.BATTLE_SERVER_SYSTEM_PREFIX + ConfigManager.BATTLE_SERVER_ID);

        gmRemoteActor = system.actorSelection(ConfigManager.REMOTE_GM_AKKA_PATH);
        // Actor name [battleServerActor]
        // battleServerActor，用于GM服务器注册、网关服务器消息处理的分发
        battleServerActor = system.actorOf(Props.create(BattleServerActor.class), GlobalConstant.BATTLE_SERVER_ACTOR_NAME);

        // 对战房间管理
        system.actorOf(Props.create(BattleRoomManagerActor.class), "battleRoomManagerActor");
    }

    @Override
    public void shutdown() {

    }

    /**
     * RemoteMsg 的分发处理
     * @param message
     * @param sender
     */
    public void handleRemoteMsg(RemoteMessage message, ActorRef sender) {
        ActorRef actor = rpcNumToHandleActorMap.get(message.getRpcNum());
        if (actor != null) {
            actor.tell(message, sender);
        } else {
            log.error("handleRemoteMsg error, unsupport rpcNum = {}", message.getRpcNum());
            RemoteMessage errorRemoteMsg = new RemoteMessage(message.getRpcNum(),
                    RemoteServer.RemoteRpcErrorCodeEnum.RemoteRpcServerError_VALUE);
            sender.tell(errorRemoteMsg, ActorRef.noSender());
        }
    }

    public void handleRequest(NetMessage message, ActorRef sender) {
        ActorRef actor = rpcNumToHandleActorMap.get(message.getRpcNum());
        if (actor != null) {
            actor.tell(message, sender);
        } else {
            log.error("handleRequest error, not support rpcNum = {}", message.getRpcNum());
            NetMessage errorNetMsg = new NetMessage(message.getRpcNum(), Rpc.RpcErrorCodeEnum.ServerError_VALUE);
            sender.tell(errorNetMsg, ActorRef.noSender());
        }
    }

    /**
     * 发送RemoteMessage到Gm服务器
     * @param msg
     */
    public void sendRemoteMsgToGm(RemoteMessage msg) {
        gmRemoteActor.tell(msg, battleServerActor);
    }

    /**
     * 通知聊天服务器创建对战聊天房间
     * @param remoteMessage
     * @param sender
     * @return
     */
    public boolean noticeChatServerCreateNewBattleChatRoom(RemoteMessage remoteMessage, ActorRef sender) {
        // 获取空闲chat服务器id
        Integer leisureChatServerId = chatServerLoadBalanceService.getLeisureChatServerId();
        if (leisureChatServerId != null) {
            // 获取该服务器akka path
            String chatServerAkkaPath = chatServerLoadBalanceService.getOneChatServerIdToAkkaPath(leisureChatServerId);
            if (chatServerAkkaPath != null) {
                // 推送
                system.actorSelection(chatServerAkkaPath).tell(remoteMessage, sender);
                log.info("【消息推送成功】 noticeChatServerCreateNewBattleChatRoom push success");
                return true;
            } else {
                log.error("【消息推送失败】 noticeChatServerCreateNewBattleChatRoom push failure, ChatServerId = {} AkkaPath ist Null", leisureChatServerId);
                return false;
            }
        }
        log.error("【消息推送失败】 noticeChatServerCreateNewBattleChatRoom push failure, ChatServerId is Null");
        return false;
    }

    /**
     * 推送网络消息给单个用户
     * @param userId
     * @param netMsg
     * @param protobufClass
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean sendNetMsgToOneUser(int userId, NetMessage netMsg, Class protobufClass) {
        log.info(
                "sendNetMsgToOneUser userId = {}, rpcNum = {}, rpcName = {}, errorCode = {}, means {}, protobuf text = {}",
                userId, netMsg.getRpcNum(), Rpc.RpcNameEnum.forNumber(netMsg.getRpcNum()), netMsg.getErrorCode(),
                Rpc.RpcErrorCodeEnum.forNumber(netMsg.getErrorCode()),
                protobufClass != null ? netMsg.getProtobufText(protobufClass) : "null");
        Integer sessionId = sessionIdService.getOneUserIdToSessionId(userId);
        if (sessionId != null) {
            return sendNetMsgToOneSession(sessionId, netMsg);
        } else {
            return false;
        }
    }

    /**
     * 发送消息到Session
     * @param sessionId
     * @param netMsg
     * @return
     */
    public boolean sendNetMsgToOneSession(int sessionId, NetMessage netMsg) {
        ActorRef gatewayResponseActor = OnlineClientManager.getInstance().getGatewayResponseActor(sessionId);
        if (gatewayResponseActor != null) {
            gatewayResponseActor.tell(netMsg, ActorRef.noSender());
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取 Actor 下属的所有 Action 对象
     * @param clazz  Actor代理对象Class
     * @return
     */
    public Map<Integer, Class<BaseMessageAction>> getActionClassByActor(Class<? extends BaseMessageActor> clazz) {
        return actorToHandleActionMap.get(clazz);
    }

    /**
     * 添加 actor 与其所属下属 action Map（一对多）
     * @param clazz  actor代理对象
     * @param map    actionMap
     */
    public void addActorToHandleAction(Class<? extends BaseMessageActor> clazz,
                                       Map<Integer, Class<BaseMessageAction>> map) {
        actorToHandleActionMap.put(clazz, map);
    }


    /**
     * 添加RpcNum通信协议号与其对应的消息处理Actor到map缓存中
     * @param rpcNum   通信协议号
     * @param actor	   处理该消息的actor代理
     */
    public void addRpcNumToHandleActorMap(int rpcNum, ActorRef actor) {
        if (rpcNumToHandleActorMap.containsKey(rpcNum) == true) {
            log.error(
                    "【addRpcNumToHandleActorMap error】 multiple actor to handle same rpcNum = {}, actorName = {} and {}",
                    rpcNum, rpcNumToHandleActorMap.get(rpcNum).getClass().getName(), actor.getClass().getName());
        }
        rpcNumToHandleActorMap.put(rpcNum, actor);
    }
}
