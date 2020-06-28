package com.jacey.game;

import com.jacey.game.client.OnlineMannager;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class Test {

    public static void main(String[] args) {
        CommonMsg.LoginRequest.Builder builder = CommonMsg.LoginRequest.newBuilder();
        builder.setUsername("aaa");
        builder.setPasswordMD5("aaaa");

        System.out.println(builder.build().toString());
    }

}
