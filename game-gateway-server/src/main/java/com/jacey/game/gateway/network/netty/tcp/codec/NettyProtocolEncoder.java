package com.jacey.game.gateway.network.netty.tcp.codec;

import com.jacey.game.common.msg.NetMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @Description: 自定义编码器
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class NettyProtocolEncoder extends MessageToByteEncoder<NetMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, NetMessage netMessage, ByteBuf byteBuf) throws Exception {
        byteBuf.writeBytes(netMessage.toBinaryMsg());
    }
}
