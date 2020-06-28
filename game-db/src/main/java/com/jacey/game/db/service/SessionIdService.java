package com.jacey.game.db.service;

import java.util.Map;

/**
 * @Description: sessionId操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface SessionIdService {

    /**
     * 添加和获取下一个可用的sessionId(redis提供的自增id)
     * @return
     */
    public int addAndGetNextAvailableSessionId();

    /**
     *
     * @param userId
     * @return
     */
    public Integer getOneUserIdToSessionId(int userId);

    /**
     *
     * @return
     */
    public Map<Integer, Integer> getAllUserIdToSessionId();

    /**
     *
     * @param userId
     * @param sessionId
     */
    public void setOneUserIdToSessionId(int userId, int sessionId);

    /**
     *
     * @param userId
     */
    public void removeOneUserIdToSessionId(int userId);

    /**
     *
     * @param sessionId
     * @return
     */
    public Integer getOneSessionIdToUserId(int sessionId);

    /**
     *
     * @param sessionId
     * @param userId
     */
    public void setOneSessionIdToUserId(int sessionId, int userId);

    /**
     *
     * @param sessionId
     */
    public void removeOneSessionIdToUserId(int sessionId);



}
