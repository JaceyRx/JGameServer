package com.jacey.game.gm.actor;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import com.jacey.game.common.annotation.MessageMethodMapping;
import com.jacey.game.common.constants.GlobalConstant;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.ChatServerLoadBalanceService;
import com.jacey.game.db.service.GatewayServerLoadBalanceService;
import com.jacey.game.db.service.LogicServerLoadBalanceService;
import com.jacey.game.db.service.impl.BattleServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.ChatServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.GatewayServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.LogicServerLoadBalanceServiceImpl;
import com.jacey.game.gm.manager.MessageManager;
import com.jacey.game.gm.manager.SpringManager;
import com.jacey.game.common.proto3.RemoteServer.*;
import com.jacey.game.common.proto3.CommonEnum.*;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 用于处理服务器之间的通讯：服务器注册、各服务器响应文本接收
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class GmActor extends BaseMessageActor {

    public GmActor() {
        super();
    }

    public GmActor(String actionPackageName) {
        super(actionPackageName);
    }

    /**
     * 在 actor 启动后，处理第一条消息之前被调用。
     */
    @Override
    public void preStart() {
        super.preStart();

        // GM服务器开启后，清空所有功能服务器的负载、地址等信息，并等待它们重新来注册
        LogicServerLoadBalanceService logicServerLoadBalanceService = SpringManager.getInstance().getBean(LogicServerLoadBalanceServiceImpl.class);
        GatewayServerLoadBalanceService gatewayServerLoadBalanceService = SpringManager.getInstance().getBean(GatewayServerLoadBalanceServiceImpl.class);
        BattleServerLoadBalanceService battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceServiceImpl.class);
        ChatServerLoadBalanceService chatServerLoadBalanceService = SpringManager.getInstance().getBean(ChatServerLoadBalanceServiceImpl.class);

        logicServerLoadBalanceService.cleanLogicServerLoadBalance();        // logic 服务器负载
        logicServerLoadBalanceService.cleanLogicServerIdToAkkaPath();       // logic akkaPath
        logicServerLoadBalanceService.setMainLogicServerId(0);              // 主 logic server Id
        gatewayServerLoadBalanceService.cleanGatewayLoadBalance();          // gateway 服务器负载
        gatewayServerLoadBalanceService.cleanGatewayIdToAkkaPath();         // gateway akkaPath
        gatewayServerLoadBalanceService.cleanGatewayIdToConnectPath();      // gateway 连接地址
        battleServerLoadBalanceService.cleanBattleServerLoadBalance();      // battle 服务器负载
        battleServerLoadBalanceService.cleanBattleServerIdToAkkaPath();     // battle akkaPath
        chatServerLoadBalanceService.cleanChatServerLoadBalance();          // chat 服务器负载
        chatServerLoadBalanceService.cleanChatServerIdToAkkaPath();         // chat akkaPath
    }

    /**
     * 服务器注册
     * @param remoteMsg		远程消息载体
     */
    @MessageMethodMapping(RemoteRpcNameEnum.RemoteRpcRegistServer_VALUE)
    public void registServer(RemoteMessage remoteMsg) {
        // 通过反射生成RegistServerRequest  protobuf消息对象
        RegistServerRequest req = remoteMsg.getLite(RegistServerRequest.class);
        // 获取发送消息的服务器信息
        RemoteServerInfo serverInfo = req.getServerInfo();
        // 获取服务器类型
        RemoteServerTypeEnum serverType = serverInfo.getServerType();
        int serverId = serverInfo.getServerId();		// 服务器id
        String akkaPath = serverInfo.getAkkaPath();		// 服务器远程调用地址
        boolean isRegistSuccess = false;				// 是否注册成功
        if (serverType == RemoteServerTypeEnum.ServerTypeLogic) {   // 逻辑服务器注册处理逻辑
            isRegistSuccess = MessageManager.getInstance().addLogicServer(serverId, akkaPath,
                    serverInfo.getIsMainLogicServer(), sender());// sender() 发送者的actorRef
        } else if (serverType == RemoteServerTypeEnum.ServerTypeBattle) {	// 对战服务器注册处理逻辑
            isRegistSuccess = MessageManager.getInstance().addBattleServer(serverId, akkaPath, sender());
        } else if (serverType == RemoteServerTypeEnum.ServerTypeChat) {   // 聊天服务器处理逻辑
            isRegistSuccess = MessageManager.getInstance().addChatServer(serverId, akkaPath, sender());
        } else if (serverType == RemoteServerTypeEnum.ServerTypeGateway) {	// 网关服务器注册逻辑
            isRegistSuccess = MessageManager.getInstance().addGateway(serverId, akkaPath,
                    serverInfo.getGatewayConnectPath(), sender());
        } else {
            log.error("【未知服务器类型】registServer error, not support server type = {}", serverType);
            sender().tell(new RemoteMessage(RemoteRpcNameEnum.RemoteRpcRegistServer_VALUE,
                    RemoteRpcErrorCodeEnum.RemoteRpcServerError_VALUE), self());
            return;
        }
        if (isRegistSuccess == true) {
            /** getContext().watch(calculator) 实现对子Actor的监管
             一旦xxxActor重启或终止，GmActor便可以接收到Terminated消息 */
            getContext().watch(sender());
            switch (serverType.getNumber()) {
                case RemoteServerTypeEnum.ServerTypeLogic_VALUE: {
                    log.info("【新的 logic 服务器已注册】 id = {}, akkaPath = {}, isMainLogicServer = {}", serverId,
                            akkaPath, serverInfo.getIsMainLogicServer());
                    break;
                } case RemoteServerTypeEnum.ServerTypeBattle_VALUE: {
                    log.info("【新的 battle 服务器已注册】 id = {}, akkaPath = {}", serverId, akkaPath);
                    break;
                } case RemoteServerTypeEnum.ServerTypeGateway_VALUE: {
                    log.info("【新的 gateway 服务器已注册】 id = {}, akkaPath = {}, connectPath = {}", serverId, akkaPath,
                            serverInfo.getGatewayConnectPath());
                    break;
                } case RemoteServerTypeEnum.ServerTypeChat_VALUE: {
                    log.info("【新的 chat 服务器已注册】 id = {}, akkaPath = {}", serverId, akkaPath);
                    break;
                } default: {
                    log.error("【未知服务器类型】after registServer success error, not support server type = {}", serverType);
                    sender().tell(new RemoteMessage(RemoteRpcNameEnum.RemoteRpcRegistServer_VALUE,
                            RemoteRpcErrorCodeEnum.RemoteRpcServerError_VALUE), self());
                    return;
                }
            }

            RegistServerResponse.Builder respBuilder = RegistServerResponse.newBuilder();
            sender().tell(new RemoteMessage(RemoteRpcNameEnum.RemoteRpcRegistServer_VALUE, respBuilder), self());
        } else {
            log.error("【服务器注册失败】 has registed, serverType = {}, akkaPath = {}", serverType, akkaPath);
            sender().tell(new RemoteMessage(RemoteRpcNameEnum.RemoteRpcRegistServer_VALUE,
                    RemoteRpcErrorCodeEnum.RemoteRpcRegistServerErrorHasRegisted_VALUE), self());
        }
    }

    /**
     * 已注册服务器断线处理
     * @param t
     * @throws Exception
     */
    @Override
    protected void doTerminated(Terminated t) throws Exception {
        ActorRef actor = t.getActor();
        String actorName = actor.path().name();
        if (GlobalConstant.GATEWAY_ACTOR_NAME.equals(actorName)) {
            if (MessageManager.getInstance().removeGateway(actor) == false) {
                log.error("doTerminated gateway is offline, but remove from MessageManager fail, akkaPath = {}",
                        actor.path());
            } else {
                log.info("【移除离线 gateway 服务器】 akkaPath = {}", actor.path().address());
            }
        } else if (GlobalConstant.LOGIC_SERVER_ACTOR_NAME.equals(actorName)) {
            if (MessageManager.getInstance().removeLogicServer(actor) == false) {
                log.error("doTerminated logic server is offline, but remove from MessageManager fail, akkaPath = {}",
                        actor.path());
            } else {
                log.info("【移除离线 logic 服务器】 akkaPath = {}", actor.path().address());
            }
        } else if (GlobalConstant.BATTLE_SERVER_ACTOR_NAME.equals(actorName)) {
            if (MessageManager.getInstance().removeBattleServer(actor) == false) {
                log.error(
                        "doTerminated battle server is offline, but remove from MessageManager fail, akkaPath = {}",
                        actor.path());
            } else {
                log.info("【移除离线 battle 服务器】 akkaPath = {}", actor.path().address());
            }
        } else if (GlobalConstant.CHAT_SERVER_ACTOR_NAME.equals(actorName)) {
            if (MessageManager.getInstance().removeChatServer(actor) == false) {
                log.error(
                        "doTerminated battle server is offline, but remove from MessageManager fail, akkaPath = {}",
                        actor.path());
            } else {
                log.info("【移除离线 Chat 服务器】 akkaPath = {}", actor.path().address());
            }
        } else {
            log.error("doTerminated error, unsupport server type, akkaPath = {}", actor.path());
        }
    }

}
