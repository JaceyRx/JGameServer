package com.jacey.game.gateway.network.netty.tcp.handle;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.gateway.actor.ChannelActor;
import com.jacey.game.gateway.manager.MessageManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @Description: netty主处理类
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class NettySocketHandle extends ChannelInboundHandlerAdapter {
    /**
     * 客户端连接时调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 判断服务可不可用
        // 不可用，则推送服务不可用消息
        if (MessageManager.getInstance().isAvailableForClient() == false) {
            CommonMsg.ForceOfflinePush.Builder builder = CommonMsg.ForceOfflinePush.newBuilder();
            builder.setForceOfflineReason(CommonEnum.ForceOfflineReasonEnum.ForceOfflineServerNotAvailable);
            NetMessage message = new NetMessage(Rpc.RpcNameEnum.ForceOfflinePush_VALUE, builder);
            Channel channel = ctx.channel();
            write(message, channel);
            ctx.close();
        }
    }

    /**
     * 当channel失效时（比如客户端断线或者服务器主动调用ctx.close），关闭channel对应的channelActor
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 获取Channel 绑定的ChannelActor
        ActorRef actor = ChannelActor.getChannelActor(ctx.channel());
        if (actor != null) {
            // PoisonPill 用于毒死通知的ChannelActor
            actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }

    /**
     * 读取的时候调用
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NetMessage netMessage = (NetMessage) msg;
        // 1.判断当前服务器是否可用
        if (MessageManager.getInstance().isAvailableForClient() == false) {
            CommonMsg.ForceOfflinePush.Builder builder = CommonMsg.ForceOfflinePush.newBuilder();
            builder.setForceOfflineReason(CommonEnum.ForceOfflineReasonEnum.ForceOfflineServerNotAvailable);
            NetMessage message = new NetMessage(Rpc.RpcNameEnum.ForceOfflinePush_VALUE, builder);
            Channel channel = ctx.channel();
            write(message, channel);
            ctx.close();
        } else {
            // 2.创建或获取Channel存储中的ChannelActor对象
            ActorRef actor = ChannelActor.attachChannelActor(ctx.channel());
            // 3.将IMessage消息转发给。该连接绑定的ChannelActor处理
            // 消息分发处理
            actor.tell(netMessage, ActorRef.noSender());
        }

    }

    /**
     * 闲置事件处理（心跳）
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.close();
        }
    }

    /**
     * 异常断开处理
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();

        if (cause.getMessage().startsWith("远程主机强迫关闭了一个现有的连接") == false) {
            InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
            log.error("【链接异常断开】, ip = {}, exception = ", insocket.getAddress().getHostAddress(), cause);
        }
    }

    private void write(IMessage message, Channel channel) {
        if (channel != null && channel.isActive() && channel.isWritable()) {
            channel.writeAndFlush(message);
        }
    }
}
