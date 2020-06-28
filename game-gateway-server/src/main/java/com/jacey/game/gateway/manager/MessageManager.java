package com.jacey.game.gateway.manager;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.jacey.game.common.constants.GlobalConstant;
import com.jacey.game.common.manager.IManager;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.ChatServerLoadBalanceService;
import com.jacey.game.db.service.GatewayServerLoadBalanceService;
import com.jacey.game.db.service.LogicServerLoadBalanceService;
import com.jacey.game.db.service.impl.ChatServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.GatewayServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.LogicServerLoadBalanceServiceImpl;
import com.jacey.game.gateway.actor.ChannelActor;
import com.jacey.game.gateway.actor.GatewayActor;
import io.netty.channel.Channel;

/**
 * @Description: 消息管理器
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class MessageManager implements IManager {

    private MessageManager(){}

    private static MessageManager instance = new MessageManager();

    public static MessageManager getInstance() {
        return instance;
    }

    /** Gateway System Actor */
    private ActorSystem system;
    /** Gateway User Actor */
    private ActorRef gatewayActor;
    /** GM Remote Actor */
    private ActorSelection gmRemoteActor;

    // 是否连接上GM服务器
    public boolean isConnectToGm = false;

    private LogicServerLoadBalanceService logicServerLoadBalanceService;
    private BattleServerLoadBalanceService battleServerLoadBalanceService;
    private GatewayServerLoadBalanceService gatewayServerLoadBalanceService;
    private ChatServerLoadBalanceService chatServerLoadBalanceService;

    @Override
    public void init() {
        logicServerLoadBalanceService = SpringManager.getInstance().getBean(LogicServerLoadBalanceServiceImpl.class);
        battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceService .class);
        gatewayServerLoadBalanceService = SpringManager.getInstance().getBean(GatewayServerLoadBalanceServiceImpl.class);
        chatServerLoadBalanceService = SpringManager.getInstance().getBean(ChatServerLoadBalanceServiceImpl.class);

        // ActorSystem name [gateway_ + gatewayId]
        system = ActorSystem.create(GlobalConstant.GATEWAY_SYSTEM_PREFIX + ConfigManager.GATEWAY_ID);

        gmRemoteActor = system.actorSelection(ConfigManager.REMOTE_GM_AKKA_PATH);
        // Actor name [gatewayActor]
        gatewayActor = system.actorOf(Props.create(GatewayActor.class), GlobalConstant.GATEWAY_ACTOR_NAME);
    }

    @Override
    public void shutdown() {

    }

    /**
     * 发送RemoteMessage到Gm服务器
     * @param msg
     */
    public void sendRemoteMsgToGm(RemoteMessage msg) {
        gmRemoteActor.tell(msg, gatewayActor);
    }

    /**
     * 发送RemoteMessage到logic服务器
     * @param msg
     * @param logicServerId
     * @return
     */
    public boolean sendRemoteMsgToLogicServer(RemoteMessage msg, int logicServerId) {
        String logicServerAkkaPath = logicServerLoadBalanceService.getOneLogicServerIdToAkkaPath(logicServerId);
        if (logicServerAkkaPath != null) {
            system.actorSelection(logicServerAkkaPath).tell(msg, gatewayActor);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 发送RemoteMessage到battle服务器
     * @param msg
     * @param battleServerId
     * @return
     */
    public boolean sendRemoteMsgToBattleServer(RemoteMessage msg, int battleServerId) {
        String battleServerAkkaPath = battleServerLoadBalanceService.getOneBattleServerIdToAkkaPath(battleServerId);
        if (battleServerAkkaPath != null) {
            system.actorSelection(battleServerAkkaPath).tell(msg, gatewayActor);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 发送RemoteMessage到battle服务器
     * @param msg
     * @param chatServerId
     * @return
     */
    public boolean sendRemoteMsgToChatServer(RemoteMessage msg, int chatServerId) {
        String chatServerAkkaPath = chatServerLoadBalanceService.getOneChatServerIdToAkkaPath(chatServerId);
        if (chatServerAkkaPath != null) {
            system.actorSelection(chatServerAkkaPath).tell(msg, gatewayActor);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 发送消息到指定对战服务器
     * 在redis中查找该客户端所在的Battle服务器进行发送，如果没有指定或者指定服务器已下线，则选取最空闲的Battle服务器让其处理
     * @param msg		消息体
     * @param sender	ResponseActor 用于接收远程服务器响应
     * @return
     */
    public boolean sendNetMsgToBattleServer(NetMessage msg, ActorRef sender) {
        ActorSelection actorSelection = null;
        int userId = msg.getUserId();
        // 获取userId对应的battleId(用于判断该用户是否在对战中)
        String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
        if (battleId != null) {
            // 根据battleId(对战id)获取所在对战服务器ID
            Integer battleServerId = battleServerLoadBalanceService.getOneBattleIdToBattleServerId(battleId);
            if (battleServerId != null) {
                // 获取该服务器的akka actor remote path供远程转发
                String akkaPath = battleServerLoadBalanceService.getOneBattleServerIdToAkkaPath(battleServerId);
                if (akkaPath != null) {
                    // 创建远程调用对象
                    actorSelection = system.actorSelection(akkaPath);
                    // 转发
                    actorSelection.tell(msg, sender);
                    return true;
                }
            }
        } else {
            return false;
        }


        /**
         *	如果通过battleId获取到的 battleServerId不可用
         *	或者通过battleServerId获取到的 akkaPath不可用
         * 	则转发到其他空闲对战服务器处理
         */
        // 获取空闲对战服务器id
        Integer leisureBattleServerId = battleServerLoadBalanceService.getLeisureBattleServerId();
        if (leisureBattleServerId != null) {
            String akkaPath = battleServerLoadBalanceService.getOneBattleServerIdToAkkaPath(leisureBattleServerId);
            if (akkaPath != null) {
                // 创建akka actor远程调用对象
                actorSelection = system.actorSelection(akkaPath);
                actorSelection.tell(msg, sender);
                return true;
            }
        }

        return false;
    }


    /**
     * 发送消息到指定对战服务器
     * 在redis中查找该客户端所在的Battle服务器进行发送，如果没有指定或者指定服务器已下线，则选取最空闲的Battle服务器让其处理
     * @param msg		消息体
     * @param sender	ResponseActor 用于接收远程服务器响应
     * @return
     */
    public boolean sendNetMsgToChatServer(NetMessage msg, ActorRef sender) {
        ActorSelection actorSelection = null;
        int userId = msg.getUserId();
        // 获取userId对应的battleId(用于判断该用户是否在对战中)
        String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
        if (battleId != null) {
            // 获取聊天服务器id
            Integer chatServerId = chatServerLoadBalanceService.getOneBattleIdToChatServerId(battleId);
            if (chatServerId != null) {
                // 获取该服务器的akka actor remote path供远程转发
                String akkaPath = chatServerLoadBalanceService.getOneChatServerIdToAkkaPath(chatServerId);
                if (akkaPath != null) {
                    // 创建远程调用对象
                    actorSelection = system.actorSelection(akkaPath);
                    // 转发
                    actorSelection.tell(msg, sender);
                    return true;
                }
            }
        } else {
            return false;
        }
        return false;
    }

    /**
     * 判断服务器当前是否可用
     * @return
     */
    public boolean isAvailableForClient() {
        return isConnectToGm && logicServerLoadBalanceService.getMainLogicServerId() > 0;
    }

    /**
     * 创建ChannelActor
     * @param channel
     * @return
     */
    public ActorRef createChannelActor(Channel channel) {
        return system.actorOf(Props.create(ChannelActor.class, channel));
    }

    /**
     * 将客户端请求转发到对应logic服务器
     * 在redis中查找该客户端所在的logic服务器进行发送，如果没有指定或者指定服务器已下线，则选取最空闲的logic服务器让其处理
     * @param msg
     * @param sender
     * @return
     */
    public boolean sendNetMsgToLogicServer(NetMessage msg, ActorRef sender) {
        ActorSelection actorSelection = null;
        int sessionId = msg.getSessionId();
        // 根据sessionId 获取其指定的消息处理Logic服务器id
        Integer logicServerId = logicServerLoadBalanceService.getOneSessionIdToLogicServerId(sessionId);
        if (logicServerId != null) {
            // 获取远程调用akka actor remote path
            String akkaPath = logicServerLoadBalanceService.getOneLogicServerIdToAkkaPath(logicServerId);
            if (akkaPath != null) {
                actorSelection = system.actorSelection(akkaPath);
                actorSelection.tell(msg, sender);	// 转发
                return true;
            }
        }
        // 如果没有指定或者指定服务器已下线，则选取最空闲的logic服务器让其处理
        // 获取 空闲逻辑服务器id
        Integer leisureLogicServerId = logicServerLoadBalanceService.getLeisureLogicServerId();
        if (leisureLogicServerId != null) {
            String akkaPath = logicServerLoadBalanceService.getOneLogicServerIdToAkkaPath(leisureLogicServerId);
            if (akkaPath != null) {
                actorSelection = system.actorSelection(akkaPath);
                actorSelection.tell(msg, sender);   // 转发
                return true;
            }
        }

        return false;
    }

    /**
     * 转发消息至【主】逻辑服务器
     * @param msg		转发消息内容
     * @param sender	responseActor，专门用于接收远程服务器响应
     * @return
     */
    public boolean sendNetMsgToMainLogicServer(NetMessage msg, ActorRef sender) {
        int mainLogicServerId = logicServerLoadBalanceService.getMainLogicServerId();
        // 判断主逻辑服务器是否正常
        if (mainLogicServerId > 0) {
            // 获取主逻辑服务器的akka actor remote path 地址
            String akkaPath = logicServerLoadBalanceService.getOneLogicServerIdToAkkaPath(mainLogicServerId);
            if (akkaPath != null) {
                // 创建远程调用对象
                ActorSelection actorSelection = system.actorSelection(akkaPath);
                // 推送消息
                actorSelection.tell(msg, sender);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 是否已经连接Gm服务器
     * @return
     */
    public boolean isAvailableForUpdateLoadBalance() {
        return isConnectToGm;
    }
}
