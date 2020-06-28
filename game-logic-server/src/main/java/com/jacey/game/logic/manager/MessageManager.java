package com.jacey.game.logic.manager;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.jacey.game.common.action.BaseMessageAction;
import com.jacey.game.common.constants.GlobalConstant;
import com.jacey.game.common.manager.IManager;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.GatewayServerLoadBalanceService;
import com.jacey.game.db.service.LogicServerLoadBalanceService;
import com.jacey.game.db.service.SessionIdService;
import com.jacey.game.db.service.impl.BattleServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.GatewayServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.LogicServerLoadBalanceServiceImpl;
import com.jacey.game.logic.actor.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 消息管理器
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

    private LogicServerLoadBalanceService logicServerLoadBalanceService;
    private GatewayServerLoadBalanceService gatewayLoadBalanceService;
    private BattleServerLoadBalanceService battleLoadBalanceService;
    private SessionIdService sessionIdService;

    /** LogicServer System Actor */
    private ActorSystem system;
    /** LogicServer User Actor */
    private ActorRef logicServerActor;
    /** GM Remote Actor */
    private ActorSelection gmRemoteActor;
    // 是否连接上GM服务器
    public boolean isConnectToGm = false;
    /**==============================<以下Map为消息处理相关>========================================*/
    /** rpcNum通信协议与对应的消息处理Actor关系缓存   key:rpcNum, value:对应处理这个消息的Actor*/
    private final Map<Integer, ActorRef> rpcNumToHandleActorMap = new HashMap<Integer, ActorRef>();
    /** Actor与其对应下属 Action的关系缓存   key:actor, value:map(key:rpcNum, value:处理这个消息的Action)*/
    private final Map<Class<? extends BaseMessageActor>, Map<Integer, Class<BaseMessageAction>>> actorToHandleActionMap = new HashMap<Class<? extends BaseMessageActor>, Map<Integer, Class<BaseMessageAction>>>();

    /** 匹配Actor */
    private ActorRef matchActor;

    /**
     *  [akka://logicServer_1@<host:port>/user/logicServerActor]
     */
    @Override
    public void init() {
        // 获取负载均衡service
        logicServerLoadBalanceService = SpringManager.getInstance().getBean(LogicServerLoadBalanceServiceImpl.class);
        gatewayLoadBalanceService = SpringManager.getInstance().getBean(GatewayServerLoadBalanceServiceImpl.class);
        battleLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceServiceImpl.class);
        sessionIdService = SpringManager.getInstance().getBean(SessionIdService.class);

        // ActorSystem name [logicServer_ + LogicServerId]
        system = ActorSystem.create(GlobalConstant.LOGIC_SERVER_SYSTEM_PREFIX + ConfigManager.LOGIC_SERVER_ID);

        gmRemoteActor = system.actorSelection(ConfigManager.REMOTE_GM_AKKA_PATH);
        // LoginActor注册，用于处理登录请求
        system.actorOf(Props.create(LoginActor.class), "loginActor");
        // 主逻辑服务器功能
        if (ConfigManager.IS_MAIN_LOGIC_SERVER == true) {
            // RegistActor注册，用于处理用户注册请求
            system.actorOf(Props.create(RegistActor.class), "registActor");
            // matchActor注册，用于处理匹配请求
            matchActor = system.actorOf(Props.create(MatchActor.class, "com.jacey.game.logic.action.match"), "matchActor");
        }
        // Actor name [logicServerActor]
        // LogicServerActor注册，用于GM服务器注册、网关服务器消息处理的分发
        logicServerActor = system.actorOf(Props.create(LogicServerActor.class), GlobalConstant.LOGIC_SERVER_ACTOR_NAME);
    }

    @Override
    public void shutdown() {

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
     * 发送RemoteMessage到Gm服务器
     * @param msg
     */
    public void sendRemoteMsgToGm(RemoteMessage msg) {
        gmRemoteActor.tell(msg, logicServerActor);
    }

    /**
     * 推送消息给gateway服务器
     * @param msg
     * @param gatewayId
     * @return
     */
    public boolean sendRemoteMsgToGataway(RemoteMessage msg, int gatewayId) {
        String gatewayAkkaPath = gatewayLoadBalanceService.getOneGatewayIdToAkkaPath(gatewayId);
        if (gatewayAkkaPath != null) {
            system.actorSelection(gatewayAkkaPath).tell(msg, logicServerActor);
            return true;
        } else {
            return false;
        }
    }

    public boolean isAvailableForUpdateLoadBalance() {
        return isConnectToGm;
    }

    /**
     * 用户消息的内部分发
     * @param message
     * @param sender
     */
    public void handleRequest(NetMessage message, ActorRef sender) {
        ActorRef actor = rpcNumToHandleActorMap.get(message.getRpcNum());
        if (actor != null) {
            actor.tell(message, sender);
        } else {
            log.error("【请求处理异常】 未知协议 rpcNum = {}", message.getRpcNum());
            NetMessage errorNetMsg = new NetMessage(message.getRpcNum(), Rpc.RpcErrorCodeEnum.ServerError_VALUE);
            sender.tell(errorNetMsg, ActorRef.noSender());
        }
    }

    /**
     * 通知BattleServer对战服务器，创建新战场
     * @param battleType
     * @param battleId
     * @param userIds
     * @return
     */
    public boolean noticeBattleServerCreateNewBattle(CommonEnum.BattleTypeEnum battleType, String battleId,
                                                     List<Integer> userIds) {
        // 获取空闲对战服务器id
        Integer leisureBattleServerId = battleLoadBalanceService.getLeisureBattleServerId();
        if (leisureBattleServerId != null) {
            // 获取该服务器 akka path
            String battleServerAkkaPath = battleLoadBalanceService.getOneBattleServerIdToAkkaPath(leisureBattleServerId);
            if (battleServerAkkaPath != null) {
                // 对战房间信息
                RemoteServer.BattleRoomInfo.Builder battleRoomInfoBuilder = RemoteServer.BattleRoomInfo.newBuilder();
                battleRoomInfoBuilder.setBattleType(battleType);	// 对战类型
                battleRoomInfoBuilder.setBattleId(battleId);		// 对战id
                battleRoomInfoBuilder.addAllUserIds(userIds);		// 对战玩家id
                // 通知对战服务器，创建对战房间消息体
                RemoteServer.NoticeBattleServerCreateNewBattleRequest.Builder builder = RemoteServer.NoticeBattleServerCreateNewBattleRequest
                        .newBuilder();
                builder.setBattleRoomInfo(battleRoomInfoBuilder);
                RemoteMessage remoteMsg = new RemoteMessage(
                        RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeBattleServerCreateNewBattle_VALUE, builder);
                // 推送
                system.actorSelection(battleServerAkkaPath).tell(remoteMsg, matchActor);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 根据SessionId发送消息给客户端
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
     * 根据UserId发送消息给客户端
     * @param userId
     * @param netMsg
     * @param protobufClass
     * @return
     */
    public boolean sendNetMsgToOneUser(int userId, NetMessage netMsg, Class protobufClass) {
        log.info(
                "【推送NetMsg消息给客户端】 userId = {}, rpcNum = {}, rpcName = {}, errorCode = {}, means {}, protobuf text = {}",
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

}
