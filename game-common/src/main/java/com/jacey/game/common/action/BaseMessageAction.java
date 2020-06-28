package com.jacey.game.common.action;

import com.google.protobuf.MessageLite;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.network.session.ISession;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 基础消息处理Action类----所有自定义消息处理Action都应该继承该类
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public abstract class BaseMessageAction {

    protected abstract void LogRequest(IMessage requestMessage) throws Exception;

    protected abstract void LogResponse(IMessage responseMessage) throws Exception;

    protected abstract IMessage doAction(IMessage requestMessage) throws Exception;

    /**
     * Action消息处理主方法
     * @param requestMessage
     * @throws Exception
     */
    public IMessage handleMessage(IMessage requestMessage) throws Exception {
        LogRequest(requestMessage);

        IMessage responseMessage = doAction(requestMessage);

        LogResponse(responseMessage);

        return responseMessage;
    }

    protected IMessage buildResponseNetMsg(int userId, int rpcName, MessageLite.Builder builder) {
        NetMessage message = new NetMessage(rpcName, builder);
        message.setUserId(userId);
        return message;
    }

}
