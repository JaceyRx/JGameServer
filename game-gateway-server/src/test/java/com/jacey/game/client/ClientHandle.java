package com.jacey.game.client;

import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class ClientHandle extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NetMessage message = (NetMessage) msg;
        switch (message.getRpcNum()) {
            case Rpc.RpcNameEnum.Login_VALUE: {
                CommonMsg.LoginResponse response = message.getLite(CommonMsg.LoginResponse.class);
                switch (message.getErrorCode()) {
                    case Rpc.RpcErrorCodeEnum.Ok_VALUE: {
                        System.out.println("响应>>>>> 登录成功 "+ message.getProtobufText(CommonMsg.LoginResponse.class));
                        break;
                    }
                    case Rpc.RpcErrorCodeEnum.LoginErrorPasswordWrong_VALUE:
                    case Rpc.RpcErrorCodeEnum.LoginErrorUsernameIsNotExist_VALUE: {
                        System.out.println("响应>>>>> 用户名或密码错误");
                        break;
                    } case Rpc.RpcErrorCodeEnum.ServerError_VALUE: {
                        System.out.println("响应>>>>> 无法注册，服务器内部错误");
                        break;
                    }
                }
                break;
            } case Rpc.RpcNameEnum.Regist_VALUE: {
                switch (message.getErrorCode()) {
                    case Rpc.RpcErrorCodeEnum.Ok_VALUE: {
                        System.out.println("响应>>>>> 注册成功");
                        break;
                    } case Rpc.RpcErrorCodeEnum.RegisErrorPasswordIllegal_VALUE: {
                        System.out.println("响应>>>>> 无法注册，密码非法");
                        break;
                    } case Rpc.RpcErrorCodeEnum.RegisErrorUsernameIllegal_VALUE: {
                        System.out.println("响应>>>>> 无法注册，用户名非法");
                        break;
                    } case Rpc.RpcErrorCodeEnum.RegisErrorUsernameIsExist_VALUE: {
                        System.out.println("响应>>>>> 无法注册，用户名已存在");
                        break;
                    } case Rpc.RpcErrorCodeEnum.ServerError_VALUE: {
                        System.out.println("响应>>>>> 无法注册，服务器内部错误");
                        break;
                    }
                }
                break;
            } case Rpc.RpcNameEnum.Match_VALUE: {
                switch (message.getErrorCode()) {
                    case Rpc.RpcErrorCodeEnum.Ok_VALUE: {
                        System.out.println("响应>>>>> 匹配成功");
                        break;
                    } default: {
                        System.out.println("响应>>>>> 匹配失败");
                    }
                }
                break;
            } case Rpc.RpcNameEnum.MatchResultPush_VALUE: {
                CommonMsg.MatchResultPush matchResultPush = message.getLite(CommonMsg.MatchResultPush.class);
                switch (message.getErrorCode()) {
                    case Rpc.RpcErrorCodeEnum.Ok_VALUE: {
                        System.out.println("响应>>>>> 匹配结果推送:" + message.getProtobufText(CommonMsg.MatchResultPush.class));
                        break;
                    } default: {
                        System.out.println("响应>>>>> xxxxx");
                    }
                }
                break;
            }
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("========ADD==========");
        OnlineMannager.getInstance().addSession(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
