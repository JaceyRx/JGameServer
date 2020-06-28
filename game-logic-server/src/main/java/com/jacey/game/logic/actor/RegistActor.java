package com.jacey.game.logic.actor;

import akka.actor.ActorRef;
import com.jacey.game.common.annotation.MessageMethodMapping;
import com.jacey.game.common.exception.RpcErrorException;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.common.utils.MD5Util;
import com.jacey.game.common.utils.StringUtil;
import com.jacey.game.db.entity.PlayUserEntity;
import com.jacey.game.db.service.PlayUserService;
import com.jacey.game.db.service.impl.PlayUserServiceImpl;
import com.jacey.game.common.constants.SystemConfigKeyConstant;
import com.jacey.game.logic.manager.SpringManager;
import com.jacey.game.logic.manager.TableConfigManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * @Description: 注册请求处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class RegistActor extends BaseMessageActor {

    private PlayUserService userService = SpringManager.getInstance().getBean(PlayUserServiceImpl.class);

    public RegistActor() {
        super();
    }

    @MessageMethodMapping(value = Rpc.RpcNameEnum.Regist_VALUE, isNet = true)
    public void doRegistActor(IMessage message) throws Exception {
        NetMessage msg = (NetMessage) message;
        CommonMsg.RegistRequest registRequest = msg.getLite(CommonMsg.RegistRequest.class);
        log.info("【req Regist】:\n{}", msg.getProtobufText(CommonMsg.RegistRequest.class));
        String username = registRequest.getUsername();
        String password = registRequest.getPassword();
        // 1.用户名称长度判断
        if (isLegalUsername(username) == false) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.RegisErrorUsernameIllegal_VALUE);
        }
        // 2.密码长度判断
        if (isLegalPassword(password) == false) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.RegisErrorPasswordIllegal_VALUE);
        }
        // 3.是否已存在用户名判断
        if (userService.hasUsername(username) == true) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.RegisErrorUsernameIsExist_VALUE);
        }

        PlayUserEntity playUserEntity = new PlayUserEntity();
        playUserEntity.setUsername(username);
        playUserEntity.setNickname(username);
        playUserEntity.setPasswordMD5(MD5Util.md5(password));
        playUserEntity.setRegistIp(msg.getUserIp());
        playUserEntity.setRegistTimestamp(new Date());
        userService.createNewUser(playUserEntity);

        CommonMsg.RegistResponse.Builder builder = CommonMsg.RegistResponse.newBuilder();
        NetMessage netMessage = new NetMessage(Rpc.RpcNameEnum.Regist_VALUE, builder);
        // 响应注册信息给client和网关服务器
        sender().tell(netMessage, ActorRef.noSender());
    }

    // username需满足SystemConfig配置的长度要求，且只能由数字或字母组成
    private boolean isLegalUsername(String username) {
        if (StringUtil.isNullOrEmpty(username) || username.length() > TableConfigManager.getInstance()
                .getSystemIntConfigByKey(SystemConfigKeyConstant.USERNAME_MAX_LENGTH)) {
            return false;
        }
        for (char c : username.toCharArray()) {
            if (StringUtil.isLetterChar(c) == false && StringUtil.isDigitChar(c) == false) {
                return false;
            }
        }
        return true;
    }

    // password需满足SystemConfig配置的长度要求，且只能由数字或字母组成
    private boolean isLegalPassword(String password) {
        if (StringUtil.isNullOrEmpty(password)
                || password.length() < TableConfigManager.getInstance()
                .getSystemIntConfigByKey(SystemConfigKeyConstant.PASSWORD_MIN_LENGTH)
                || password.length() > TableConfigManager.getInstance()
                .getSystemIntConfigByKey(SystemConfigKeyConstant.PASSWORD_MAX_LENGTH)) {
            return false;
        }
        for (char c : password.toCharArray()) {
            if (StringUtil.isLetterChar(c) == false && StringUtil.isDigitChar(c) == false) {
                return false;
            }
        }

        return true;
    }

}
