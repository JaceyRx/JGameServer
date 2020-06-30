package com.jacey.game.gateway.network.netty.websocket.handle;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.gateway.actor.ChannelActor;
import com.jacey.game.gateway.manager.MessageManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @Description: netty主处理类
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class NettyWebSocketHandle extends SimpleChannelInboundHandler<WebSocketFrame> {

    private ByteBuf tempByteBuf;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, WebSocketFrame webSocketFrame) throws Exception {
        if (webSocketFrame instanceof TextWebSocketFrame) {
            channelHandlerContext.close();
        } else if (webSocketFrame instanceof PingWebSocketFrame) {
            // websocket自带心跳恢复 PING / PONG
            channelHandlerContext.channel().write(new PongWebSocketFrame(webSocketFrame.content()));
        } else if (webSocketFrame instanceof BinaryWebSocketFrame) {
            ByteBuf in = webSocketFrame.content();
            // 拆包黏包处理
            if (webSocketFrame.isFinalFragment() == false) {
                // 由于不是最后一个数据包，所以讲当前数据包临时存储到 tempByteBuf 中
                if (tempByteBuf == null) {
                    tempByteBuf = channelHandlerContext.alloc().heapBuffer();
                }
                tempByteBuf.writeBytes(in);
            } else {
                handleMessage(in, channelHandlerContext.channel());
            }
        } else if (webSocketFrame instanceof ContinuationWebSocketFrame) {
            // 后续数据包接收
            tempByteBuf.writeBytes(webSocketFrame.content());
            if (webSocketFrame.isFinalFragment() == true) {
                handleMessage(tempByteBuf, channelHandlerContext.channel());
                tempByteBuf.clear();
            }
        } else {
            log.error("【WebSocket消息读取错误】 not support webSocketFrame type = {}", webSocketFrame.getClass().getName());
            channelHandlerContext.close();
        }
    }

    public void handleMessage(ByteBuf byteBuf, Channel channel) {
        // 1.判断当前服务器是否可用
        if (MessageManager.getInstance().isAvailableForClient() == false) {
            CommonMsg.ForceOfflinePush.Builder builder = CommonMsg.ForceOfflinePush.newBuilder();
            builder.setForceOfflineReason(CommonEnum.ForceOfflineReasonEnum.ForceOfflineServerNotAvailable);
            NetMessage message = new NetMessage(Rpc.RpcNameEnum.ForceOfflinePush_VALUE, builder);
            write(message, channel);
            channel.close();
        } else {
            // 2.创建或获取Channel存储中的ChannelActor对象
            ActorRef actor = ChannelActor.attachChannelActor(channel);
            int totalLength = byteBuf.readInt();
            int rpcNum = byteBuf.readInt();
            // errorCode
            byteBuf.readInt();
            byte[] bytes = new byte[totalLength - 12];
            byteBuf.readBytes(bytes);
            NetMessage message = new NetMessage(rpcNum, bytes);
            // 3.将IMessage消息转发给。该连接绑定的ChannelActor处理
            // 消息分发处理
            actor.tell(message, ActorRef.noSender());
        }
    }

    private void write(IMessage message, Channel channel) {
        if (channel != null && channel.isActive() && channel.isWritable()) {
            channel.writeAndFlush(new BinaryWebSocketFrame(message.toBinaryMsg()));
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

}
