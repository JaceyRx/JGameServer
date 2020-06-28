package com.jacey.game.common.msg;

import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TextFormat;
import com.jacey.game.common.network.session.ISession;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 通过网络传输的消息对象
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
@Getter
@Setter
public class NetMessage extends AbstractMessage {

    /** 消息内容body */
    private byte[] data;
    /** 错误代码 */
    private int errorCode;
    /** 自定义SessionId */
    private int sessionId;
    /** 自定义SessionId */
    private ISession session;
    /** 用户id */
    private int userId;
    /** 用户登录ip */
    private String userIp;

    public NetMessage() {}

    public NetMessage(int RpcNum, MessageLite lite) {
        this.rpcNum = RpcNum;
        this.errorCode = Rpc.RpcErrorCodeEnum.Ok_VALUE;
        this.data = lite.toByteArray();
    }

    public NetMessage(int RpcNum, MessageLite.Builder builder) {
        this.rpcNum = RpcNum;
        this.errorCode = Rpc.RpcErrorCodeEnum.Ok_VALUE;
        this.data = builder.build().toByteArray();
    }

    public NetMessage(int RpcNum, byte[] data) {
        this.rpcNum = RpcNum;
        this.errorCode = Rpc.RpcErrorCodeEnum.Ok_VALUE;
        this.data = data;
    }

    public NetMessage(int RpcNum, int errorCode) {
        this.rpcNum = RpcNum;
        this.errorCode = errorCode;
        this.data = null;
    }

    /**
     * 获取数据长度
     * @return
     */
    public int getDataLength() {
        return data == null ? 0 : data.length;
    }

    /**
     * 获取数据包总长度
     * 包含总字节数、协议名数字、errorCode（前3部分每部分都是4字节）、protobuf二进制部分
     * @return
     */
    public int getTotalLength() {
        return 12 + getDataLength();
    }

    /**
     *
     * @return
     */
    @Override
    public ByteBuf toBinaryMsg() {
        int totalLength = this.getTotalLength();
        ByteBuf out = Unpooled.directBuffer(totalLength);
        out.writeInt(totalLength);
        out.writeInt(this.rpcNum);
        out.writeInt(this.errorCode);
        if (data != null) {
            out.writeBytes(data);
        }
        return out;
    }

    /** 内部类，用于缓存加载过的protobuf Lite */
    private static class MessageLiteCache {
        static final Map<String, MessageLite> cache = new HashMap<String, MessageLite>();

        private MessageLiteCache() {
        }
    }

    /**
     * 通过反射生成protobuf对象
     * @param clz
     * @param <T>
     * @return
     */
    public <T> T getLite(Class<T> clz) {
        try {
            MessageLite prototype = MessageLiteCache.cache.get(clz.getName());
            if (prototype == null) {
                // 反射生成protobuf对象
                Method method = clz.getMethod("getDefaultInstance");
                prototype = (MessageLite) method.invoke(null);
                MessageLiteCache.cache.put(clz.getName(), prototype);
            }
            if (prototype != null) {
                return (T) prototype.newBuilderForType().mergeFrom(data).buildPartial();
            }
        } catch (Throwable t) {
            log.error("getLite error = ", t);
        }
        return null;
    }

    /**
     * 获取protobuf 文本
     * @param clz
     * @param <T>
     * @return
     */
    public <T extends MessageOrBuilder> String getProtobufText(Class<T> clz) {
        try {
            MessageLite prototype = MessageLiteCache.cache.get(clz.getName());
            if (prototype == null) {
                Method method = clz.getMethod("getDefaultInstance");
                prototype = (MessageLite) method.invoke(null);
                MessageLiteCache.cache.put(clz.getName(), prototype);
            }
            if (prototype != null) {
                return TextFormat.printToUnicodeString((T) prototype.newBuilderForType().mergeFrom(data));
            }
        } catch (Throwable t) {
            log.error("getProtobufText error = ", t);
        }

        return null;
    }


}
