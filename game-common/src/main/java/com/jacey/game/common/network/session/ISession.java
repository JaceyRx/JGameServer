package com.jacey.game.common.network.session;

import com.jacey.game.common.msg.IMessage;

/**
 * @Description: 自定义Session.结构声明接口
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface ISession {

    /**
     * 写数据
     * @param message
     */
    void write(IMessage message);

    /**
     * 关闭session
     */
    void close();

    /**
     * 获取ip
     * @return
     */
    String getRemotePath();

    /**
     * 获取sessionId
     * @return
     */
    long getSessionId();

    /**
     * 根据key获取数据
     * @param key
     * @return
     */
    Object getData(String key);

    /**
     * 存储数据
     * @param key
     * @param value
     */
    void putData(String key, Object value);

    /**
     * 是否含有数据
     * @param key
     * @return
     */
    boolean hasData(String key);

}
