package com.jacey.game.common.msg;

import io.netty.buffer.ByteBuf;

/**
 * @Description: IMessage 接口方法的抽象实现
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public abstract class AbstractMessage implements IMessage {

    protected int rpcNum;
    protected Object lite;

    @Override
    public int getRpcNum() {
        return rpcNum;
    }

    @Override
    public void setRpcNum(int rpcNum) {
        this.rpcNum = rpcNum;
    }

    @Override
    public Object getLite() {
        return lite;
    }

    @Override
    public void setLite(Object lite) {
        this.lite = lite;
    }

    @Override
    public ByteBuf toBinaryMsg() {
        return null;
    }
}
