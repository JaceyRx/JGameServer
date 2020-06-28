package com.jacey.game.common.msg;

import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TextFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 服务器之间通选的消息载体，用于GM服务器、逻辑服务器、对战服务器、网关服务器之间的通讯
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
@Getter
@Setter
public class RemoteMessage extends AbstractMessage {

    /** 消息内容body */
    private byte[] data;
    /** 错误代码 */
    private int errorCode;

    public RemoteMessage() {
    }

    public RemoteMessage(int RpcNum, MessageLite lite) {
        this.rpcNum = RpcNum;
        this.errorCode = 0;
        this.data = lite.toByteArray();
    }

    public RemoteMessage(int RpcNum, MessageLite.Builder builder) {
        this.rpcNum = RpcNum;
        this.errorCode = 0;
        this.data = builder.build().toByteArray();
    }

    public RemoteMessage(int RpcNum, byte[] data) {
        this.rpcNum = RpcNum;
        this.errorCode = 0;
        this.data = data;
    }

    public RemoteMessage(int RpcNum, int errorCode) {
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
