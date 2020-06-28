package com.jacey.game.logic.actor;

import akka.actor.*;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.LocalMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.LocalServer;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.db.entity.PlayStateEntity;
import com.jacey.game.db.service.LogicServerLoadBalanceService;
import com.jacey.game.db.service.PlayStateService;
import com.jacey.game.db.service.impl.PlayStateServiceImpl;
import com.jacey.game.logic.manager.ConfigManager;
import com.jacey.game.logic.manager.MessageManager;
import com.jacey.game.logic.manager.OnlineClientManager;
import com.jacey.game.logic.manager.SpringManager;
import com.jacey.game.logic.service.MatchService;
import com.jacey.game.logic.service.impl.MatchServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * @Description: 主要用于GM服务器注册、网关服务器消息处理的分发
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class LogicServerActor  extends UntypedAbstractActor {

    private Cancellable logicServerReconnectGmSchedule = null;

    private LogicServerLoadBalanceService loadBalanceService = SpringManager.getInstance().getBean(LogicServerLoadBalanceService.class);
    private PlayStateService playStateService = SpringManager.getInstance().getBean(PlayStateServiceImpl.class);
    private MatchService matchService = SpringManager.getInstance().getBean(MatchServiceImpl.class);
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

    /**
     * 消息处理主入口
     * @param o
     * @throws Throwable
     */
    @Override
    public void onReceive(Object o) throws Throwable {
        if (o instanceof Terminated) {
            doTerminated((Terminated) o);
        } else if (o instanceof NetMessage) {
            /** 客户端请求处理 */
            NetMessage netMessage = (NetMessage) o;
            /** 消息分发 */
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
                    serverInfoBuidle.setServerType(CommonEnum.RemoteServerTypeEnum.ServerTypeLogic);  // 服务器类型
                    serverInfoBuidle.setServerId(ConfigManager.LOGIC_SERVER_ID);                             // 服务器id
                    serverInfoBuidle.setAkkaPath(ConfigManager.LOGIC_SERVER_AKKA_PATH);                      // 服务器 akka path
                    serverInfoBuidle.setIsMainLogicServer(ConfigManager.IS_MAIN_LOGIC_SERVER);         // 是否是主逻辑服务器
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
                        loadBalanceService.setOneLogicServerLoadBalance(ConfigManager.LOGIC_SERVER_ID,
                                OnlineClientManager.getInstance().getOnlineActorCount());

                    } else {
                        log.error("【GM服务器注册失败】, errorCodeEnum = {}", errorCodeEnum);
                        System.exit(0);
                    }
                    break;
                } case RemoteServer.RemoteRpcNameEnum.RemoteRpcGatewayNoticeClientOfflinePush_VALUE: {
                    /** gateway监测到客户端离线后通知逻辑服、战斗服 */
                    RemoteServer.GatewayNoticeClientOfflinePush push = remoteMessage.getLite(RemoteServer.GatewayNoticeClientOfflinePush.class);
                    if (push.getUserId() != 0 && push.getIsUserOffline() == true) {
                        // 已登录用于的断线处理
                        int offlineUserId = push.getUserId();
                        // 如果玩家正在匹配，强制结束匹配
                        // 因为只有主逻辑服务器处理匹配请求。所以需要判断当前服务器是否是主服务器
                        if (ConfigManager.IS_MAIN_LOGIC_SERVER == true) {
                            PlayStateEntity playStateEntity = playStateService.getPlayStateByUserId(offlineUserId);
                            if (playStateEntity.getUserActionState() == CommonEnum.UserActionStateEnum.Matching_VALUE) {
                                // 移除匹配队列
                                matchService.removeMatchPlayer(offlineUserId,
                                        CommonEnum.BattleTypeEnum.forNumber(playStateEntity.getBattleType()));

                            }
                        }
                    }
                    int offlineSessionId = push.getSessionId();
//                    OnlineClientManager.getInstance().removeUserActor(offlineSessionId);
                    OnlineClientManager.getInstance().removeSessionIdToGatewayResponseActor(offlineSessionId);
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
