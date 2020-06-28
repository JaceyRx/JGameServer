package com.jacey.game.gateway.actor;

import akka.actor.*;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.LocalMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.LocalServer.LocalRpcNameEnum;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.service.GatewayServerLoadBalanceService;
import com.jacey.game.db.service.impl.GatewayServerLoadBalanceServiceImpl;
import com.jacey.game.gateway.manager.ConfigManager;
import com.jacey.game.gateway.manager.MessageManager;
import com.jacey.game.gateway.manager.OnlineClientManager;
import com.jacey.game.gateway.manager.SpringManager;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.Duration;

import java.rmi.Remote;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class GatewayActor extends UntypedAbstractActor {

    private Cancellable gatewayReconnectGmSchedule = null;

    private GatewayServerLoadBalanceService gatewayServerLoadBalanceService = SpringManager.getInstance().getBean(GatewayServerLoadBalanceServiceImpl.class);


    /**
     * GatewayActor 启动时调用
     * @throws Exception
     */
    @Override
    public void preStart() throws Exception {
        // 开启定时任务，每5秒尝试与GM服务器进行通讯，直到连接成功
        if (gatewayReconnectGmSchedule == null) {
            gatewayReconnectGmSchedule = schedule(0, 5,
                    new LocalMessage(LocalRpcNameEnum.LocalRpcRegistToGmServer_VALUE));
        }
    }


    @Override
    public void onReceive(Object o) throws Throwable {
        if (o instanceof Terminated) {
            doTerminated((Terminated) o);
        } else if (o instanceof LocalMessage) {
            /** 执行GM服务器注册 */
            LocalMessage localMessage = (LocalMessage) o;
            switch (localMessage.getRpcNum()) {
                /** GM注册发起协议 */
                case LocalRpcNameEnum.LocalRpcRegistToGmServer_VALUE: {
                    log.info("【正在尝试连接GM服务器....】");
                    // 构造服务器注册请求消息体
                    RemoteServer.RegistServerRequest.Builder registServerRequest = RemoteServer.RegistServerRequest.newBuilder();
                    // 构造服务器信息 消息体
                    RemoteServer.RemoteServerInfo.Builder serverInfoBuidle = RemoteServer.RemoteServerInfo.newBuilder();
                    serverInfoBuidle.setServerType(CommonEnum.RemoteServerTypeEnum.ServerTypeGateway);  // 服务器类型
                    serverInfoBuidle.setServerId(ConfigManager.GATEWAY_ID);                             // 服务器id
                    serverInfoBuidle.setAkkaPath(ConfigManager.GATEWAY_AKKA_PATH);                      // 服务器 akka path
                    serverInfoBuidle.setGatewayConnectPath(ConfigManager.GATEWAY_CONNECT_PATH);         // 网关服务器连接ip
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
                        if (gatewayReconnectGmSchedule != null) {
                            gatewayReconnectGmSchedule.cancel();
                            gatewayReconnectGmSchedule = null;
                        }
                        // 连接GM成功后，更新当前服务负载信息
                        gatewayServerLoadBalanceService.setOneGatewayLoadBalance(ConfigManager.GATEWAY_ID,
                                OnlineClientManager.getInstance().getOnlineSessionCount());

                    } else {
                        log.error("【GM服务器注册失败】, errorCodeEnum = {}", errorCodeEnum);
                        System.exit(0);
                    }
                    break;
                }
                // logicServer在发现同一账号二次登录时，通知旧session所在的gateway进行强制下线
                case RemoteServer.RemoteRpcNameEnum.RemoteRpcLogicServerNoticeGatewayForceOfflineClient_VALUE: {
                    RemoteServer.LogicServerNoticeGatewayForceOfflineClientPush forceOfflineClientPush = remoteMessage
                            .getLite(RemoteServer.LogicServerNoticeGatewayForceOfflineClientPush.class);
                    int oldSessionId = forceOfflineClientPush.getSessionId();
                    Channel channel = OnlineClientManager.getInstance().getChannel(oldSessionId);
                    if (channel == null) {
                        log.error(
                                "handle LogicServerNoticeGatewayForceOfflineClient, but can't find channel, sessionId = {}",
                                oldSessionId);
                    } else {
                        CommonMsg.ForceOfflinePush.Builder pushBuilder = CommonMsg.ForceOfflinePush.newBuilder();
                        pushBuilder.setForceOfflineReason(forceOfflineClientPush.getForceOfflineReason());
                        NetMessage netMsg = new NetMessage(Rpc.RpcNameEnum.ForceOfflinePush_VALUE, pushBuilder);
                        write(netMsg, channel);
                        // 断开连接 Netty Handle的channelInactive() 会进行断线后续操作
                        channel.close();
                    }
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
        if (gatewayReconnectGmSchedule == null) {
            gatewayReconnectGmSchedule = schedule(0, 5,
                    new LocalMessage(LocalRpcNameEnum.LocalRpcRegistToGmServer_VALUE));
        }
    }

    private void write(IMessage message, Channel channel) {
        if (channel != null && channel.isActive() && channel.isWritable()) {
            channel.writeAndFlush(message);
        }
    }
}
