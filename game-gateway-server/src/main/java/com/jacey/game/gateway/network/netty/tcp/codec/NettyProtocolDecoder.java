package com.jacey.game.gateway.network.netty.tcp.codec;

import com.jacey.game.common.msg.NetMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @Description: 自定义解码器
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class NettyProtocolDecoder extends ByteToMessageDecoder {

    private static final int HEADER_LENGTH = 12;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        int dataPackLength = byteBuf.readableBytes();
        if (dataPackLength < HEADER_LENGTH) {
            return;
        }
        // ----------------消息协议格式-------------------------
        // packetLength | rpcNum | errorCode | body
        // int             int      int       byte[]
        // 打标记
        byteBuf.markReaderIndex();

        int totalLength = byteBuf.readInt();
        // 数据包长度小于 packetLength。才读取(拆包)
        if (dataPackLength >= totalLength) {
            int rpcNum = byteBuf.readInt();
            int errorCode = byteBuf.readInt();
            byte[] bytes = new byte[totalLength - HEADER_LENGTH];
            byteBuf.readBytes(bytes);
            NetMessage message = new NetMessage(rpcNum, bytes);
            message.setErrorCode(errorCode);
            list.add(message);
        } else {
            // 回滚到标记点
            byteBuf.resetReaderIndex();
            return;
        }

    }
}
