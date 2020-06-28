package com.jacey.game.battle.actor;

import akka.actor.*;
import com.jacey.game.battle.manager.ConfigManager;
import com.jacey.game.battle.manager.MessageManager;
import com.jacey.game.battle.manager.SpringManager;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.LocalMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.LocalServer;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.battle.manager.OnlineClientManager;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.impl.BattleServerLoadBalanceServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * @Description: 用于GM服务器的注册与远程消息的接收
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class BattleServerActor extends UntypedAbstractActor {

    private Cancellable logicServerReconnectGmSchedule = null;

    private BattleServerLoadBalanceService battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceServiceImpl.class);

    /**
     * GatewayActor 启动时调用
     * @throws Exception
     */
    @Override
    public void preStart() throws Exception {
        // 开启定时任务，每5秒尝试与GM服务器进行通讯，直到连接成功
        if (logicServerReconnectGmSchedule == null) {
            logicServerReconnectGmSchedule = schedule(0, 5,
                    new LocalMessage(LocalServer.LocalRpcNameEnum.LocalRpcRegistToGmServer_VALUE));
        }
    }


    @Override
    public void onReceive(Object o) throws Throwable {
        if (o instanceof Terminated) {
            doTerminated((Terminated) o);
        } else if (o instanceof NetMessage) {		// 客户端对战请求
            NetMessage netMessage = (NetMessage) o;
            log.info("请求拉： rpcNum = {}", netMessage.getRpcNum());
            MessageManager.getInstance().handleRequest(netMessage, sender());
        } else if (o instanceof LocalMessage) {
            /** 执行GM服务器注册 */
            LocalMessage localMessage = (LocalMessage) o;
            switch (localMessage.getRpcNum()) {
                /** GM注册发起协议 */
                case LocalServer.LocalRpcNameEnum.LocalRpcRegistToGmServer_VALUE: {
                    log.info("【正在尝试连接GM服务器....】");
                    // 构造服务器注册请求消息体
                    RemoteServer.RegistServerRequest.Builder registServerRequest = RemoteServer.RegistServerRequest.newBuilder();
                    // 构造服务器信息 消息体
                    RemoteServer.RemoteServerInfo.Builder serverInfoBuidle = RemoteServer.RemoteServerInfo.newBuilder();
                    serverInfoBuidle.setServerType(CommonEnum.RemoteServerTypeEnum.ServerTypeBattle);         // 服务器类型
                    serverInfoBuidle.setServerId(ConfigManager.BATTLE_SERVER_ID);                             // 服务器id
                    serverInfoBuidle.setAkkaPath(ConfigManager.BATTLE_SERVER_AKKA_PATH);                      // 服务器 akka path
                    registServerRequest.setServerInfo(serverInfoBuidle);
                    // 构造Remote 消息传输消息体
                    RemoteMessage message = new RemoteMessage(
                            RemoteServer.RemoteRpcNameEnum.RemoteRpcRegistServer_VALUE,
                            registServerRequest);
                    // 推送给GM服务器
                    MessageManager.getInstance().sendRemoteMsgToGm(message);

                    break;
                }
                default: {
                    log.error("【LocalMessage解析错误】 not support localMessage rpcNum = {}",
                            localMessage.getRpcNum());
                    break;
                }
            }
        } else if (o instanceof RemoteMessage){
            /** 服务器之间通讯消息处理 */
            RemoteMessage remoteMessage = (RemoteMessage) o;
            int errorCode = remoteMessage.getErrorCode();
            RemoteServer.RemoteRpcErrorCodeEnum errorCodeEnum = RemoteServer.RemoteRpcErrorCodeEnum.forNumber(errorCode);
            // 协议类型判断
            switch (remoteMessage.getRpcNum()) {
                case RemoteServer.RemoteRpcNameEnum.RemoteRpcRegistServer_VALUE: {
                    /** 服务器注册响应 */
                    // 根据返回的errorCode判断是否注册成功
                    if (errorCode == RemoteServer.RemoteRpcErrorCodeEnum.RemoteRpcOk_VALUE) {
                        // 开启 actor remote 监听
                        context().watch(sender());
                        // 修改MessageManager注册状态标记
                        MessageManager.getInstance().isConnectToGm = true;
                        log.info("【向GM服务器注册成功....】");
                        if (logicServerReconnectGmSchedule != null) {
                            logicServerReconnectGmSchedule.cancel();
                            logicServerReconnectGmSchedule = null;
                        }
                        // 连接GM成功后，更新当前服务负载信息
                        battleServerLoadBalanceService.setOneBattleServerLoadBalance(ConfigManager.BATTLE_SERVER_ID,
                                OnlineClientManager.getInstance().getBattleCount());

                    } else {
                        log.error("【GM服务器注册失败】, errorCodeEnum = {}", errorCodeEnum);
                        System.exit(0);
                    }
                    break;
                } case RemoteServer.RemoteRpcNameEnum.RemoteRpcGatewayNoticeClientOfflinePush_VALUE: {
                    // 客户端离线通知
                    RemoteServer.GatewayNoticeClientOfflinePush push = remoteMessage.getLite(RemoteServer.GatewayNoticeClientOfflinePush.class);
                    int sessionId = push.getSessionId();
                    // 只移除GatewayResponseActor
                    OnlineClientManager.getInstance().removeSessionIdToGatewayResponseActor(sessionId);
                    break;
                } case RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeBattleServerCreateNewBattle_VALUE: {
                    // 创建对战战场
                    MessageManager.getInstance().handleRemoteMsg(remoteMessage, sender());
                    break;
                }
            }
        } else {
            log.error("【msg 解析错误】not support msg type = {}", o.getClass().getName());
        }
    }

    /**
     * Actor定时推送任务
     * @param initialDelaySecond    延迟时间（单位：秒）
     * @param intervalSecond        间隔时间（单位：秒）
     * @param msg                   定时发送的消息
     * @return
     */
    private Cancellable schedule(int initialDelaySecond, int intervalSecond, IMessage msg) {
        // 获取当前Actor的ActorSystem，
        ActorSystem system = context().system();
        // 定时通知当前Actor，本地消息体LocalMessage，让其发起GM注册远程操作
        return system.scheduler().schedule(Duration.create(initialDelaySecond, TimeUnit.SECONDS),
                Duration.create(intervalSecond, TimeUnit.SECONDS), getSelf(), msg, system.dispatcher(),
                ActorRef.noSender());
    }

    /**
     * 监测到GM断线后，开启定时任务，尝试重新连接
     * @param t
     * @throws Exception
     */
    private void doTerminated(Terminated t) throws Exception {
        MessageManager.getInstance().isConnectToGm = false;
        log.warn("【GM服务器连接已断开...】, 开始重连任务, 执行间隔 = 5s");
        if (logicServerReconnectGmSchedule == null) {
            logicServerReconnectGmSchedule = schedule(0, 5,
                    new LocalMessage(LocalServer.LocalRpcNameEnum.LocalRpcRegistToGmServer_VALUE));
        }
    }

}
