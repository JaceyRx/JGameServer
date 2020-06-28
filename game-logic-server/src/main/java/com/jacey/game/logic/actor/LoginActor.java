package com.jacey.game.logic.actor;

import akka.actor.ActorRef;
import com.jacey.game.common.annotation.MessageMethodMapping;
import com.jacey.game.common.exception.RpcErrorException;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.common.proto3.RemoteServer.LogicServerNoticeGatewayForceOfflineClientPush;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.common.utils.DateTimeUtil;
import com.jacey.game.common.utils.StringUtil;
import com.jacey.game.db.service.GatewayServerLoadBalanceService;
import com.jacey.game.db.service.PlayStateService;
import com.jacey.game.db.service.PlayUserService;
import com.jacey.game.db.service.SessionIdService;
import com.jacey.game.db.service.impl.GatewayServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.PlayStateServiceImpl;
import com.jacey.game.db.service.impl.PlayUserServiceImpl;
import com.jacey.game.db.service.impl.SessionIdServiceImpl;
import com.jacey.game.logic.manager.MessageManager;
import com.jacey.game.logic.manager.OnlineClientManager;
import com.jacey.game.logic.manager.SpringManager;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 登录请求处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class LoginActor extends BaseMessageActor {

    private PlayUserService userService = SpringManager.getInstance().getBean(PlayUserServiceImpl.class);
    private PlayStateService playStateService = SpringManager.getInstance().getBean(PlayStateServiceImpl.class);

    private GatewayServerLoadBalanceService gatewayLoadBalanceService = SpringManager.getInstance().getBean(GatewayServerLoadBalanceServiceImpl.class);
    private SessionIdService sessionIdService = SpringManager.getInstance().getBean(SessionIdServiceImpl.class);

    public LoginActor() {
        super();
    }

    @MessageMethodMapping(value = Rpc.RpcNameEnum.Login_VALUE, isNet = true)
    public void doLoginActor(IMessage message) throws Exception {
        NetMessage msg = (NetMessage) message;
        int sessionId = msg.getSessionId();
        CommonMsg.LoginRequest loginRequest = msg.getLite(CommonMsg.LoginRequest.class);
        log.info("【登录请求】:\n{}", msg.getProtobufText(CommonMsg.LoginRequest.class));
        String username = loginRequest.getUsername();
        String passwordMD5 = loginRequest.getPasswordMD5();
        // 1.判断登录用户名和密码是否为空
        if (StringUtil.isNullOrEmpty(username) || StringUtil.isNullOrEmpty(passwordMD5)) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.ClientError_VALUE);
        }
        // 2.判断用户是否存在
        Integer userId = userService.getUserIdByUsername(username);
        if (userId == null) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.LoginErrorUsernameIsNotExist_VALUE);
        }
        // 3.密码是否正确
        CommonMsg.UserData userData = userService.getUserDataByUserId(userId);
        if (userData.getPasswordMD5().equals(passwordMD5.toUpperCase()) == false) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.LoginErrorPasswordWrong_VALUE);
        }
        CommonMsg.UserData.Builder userDataBuilder = userData.toBuilder();
        // 4.判断是否被封禁
        if (userData.hasForbidInfo() == true) {
            CommonMsg.UserForbidInfo forbidInfo = userData.getForbidInfo();
            if (DateTimeUtil.getCurrentTimestamp() < forbidInfo.getForbidEndTimestamp()) {
                throw new RpcErrorException(Rpc.RpcErrorCodeEnum.LoginErrorForbid_VALUE);
            } else {
                userDataBuilder.clearForbidInfo();
            }
        }
        // 5.判断是否同一账号二次登录
        Integer oldSessionId = sessionIdService.getOneUserIdToSessionId(userId);
        if (oldSessionId != null) {
            // 通知旧session所在的gateway踢掉之前登录的账号
            Integer gatewayId = gatewayLoadBalanceService.getOneSessionIdToGatewayId(sessionId);
            if (gatewayId == null) {
                log.error("【登录异常】 无法找到 old session 所对应的 gatewayId, userId = {}, old sessionId = {}", userId,
                        oldSessionId);
            } else {
                // 强制下线通知消息体
                LogicServerNoticeGatewayForceOfflineClientPush.Builder pushBuilder = LogicServerNoticeGatewayForceOfflineClientPush.newBuilder();
                pushBuilder.setSessionId(oldSessionId);
                pushBuilder.setForceOfflineReason(CommonEnum.ForceOfflineReasonEnum.ForceOfflineSameUserLogin);
                RemoteMessage remoteMessage = new RemoteMessage(
                        RemoteServer.RemoteRpcNameEnum.RemoteRpcLogicServerNoticeGatewayForceOfflineClient_VALUE, pushBuilder);
                if (MessageManager.getInstance().sendRemoteMsgToGataway(remoteMessage, gatewayId) == false) {
                    log.error("【消息推送异常】 无法推送消到 gateway Server, userId = {}, old sessionId = {}, gatewayId = {}",
                            userId, oldSessionId, gatewayId);
                }
            }
        }
        // 6.记录sessioId与userId的对应关系
        sessionIdService.setOneUserIdToSessionId(userId, sessionId);
        sessionIdService.setOneSessionIdToUserId(sessionId, userId);

        // 7.记录该玩家在gateway的ResponseActor，并为其创建UserActor
        OnlineClientManager.getInstance().addSessionIdToGatewayResponseActor(sessionId, sender());
        // 8.修改玩家状态
        playStateService.changeUserOnlineState(userId, true);
        // 9.更新本次登录信息
        userDataBuilder.setLastLoginIp(msg.getUserIp());
        userDataBuilder.setLastLoginTimestamp(DateTimeUtil.getCurrentTimestamp());
        userData = userDataBuilder.build();
        userService.update(userData);
        // 9.构造响应消息体
        CommonMsg.LoginResponse.Builder builder = CommonMsg.LoginResponse.newBuilder();
        builder.setUserInfo(userService.getUserInfoByUserId(userId));
        NetMessage respMsg = new NetMessage(Rpc.RpcNameEnum.Login_VALUE, builder);
        respMsg.setUserId(userId);
        log.info("【登录响应】:\n{}", respMsg.getProtobufText(CommonMsg.LoginResponse.class));
        // 10.响应
        sender().tell(respMsg, ActorRef.noSender());
    }


}
