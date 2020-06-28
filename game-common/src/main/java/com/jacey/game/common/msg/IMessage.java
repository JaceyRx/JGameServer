package com.jacey.game.common.msg;

import io.netty.buffer.ByteBuf;

/**
 * @Description: 消息载体结构声明
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface IMessage {

    /**
     * 获取协议号
     * @return
     */
    int getRpcNum();

    /**
     * 设置协议编号
     * @param rpcNum
     */
    void setRpcNum(int rpcNum);

    /**
     * 获取protobuf Lite对象
     * @return
     */
    Object getLite();

    /**
     * 设置protobuf Lite对象
     * @param lite
     */
    void setLite(Object lite);

    /**
     * 转换成传输的二进制消息
     * @return
     */
    ByteBuf toBinaryMsg();

}
