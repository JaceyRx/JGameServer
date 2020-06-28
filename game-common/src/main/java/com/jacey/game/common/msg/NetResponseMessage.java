package com.jacey.game.common.msg;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 用于ChannelActor 区分远程服务器响应
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Getter
@Setter
public class NetResponseMessage {

    private NetMessage netMessage;

    public NetResponseMessage(NetMessage netMessage) {
        this.netMessage = netMessage;
    }

}
