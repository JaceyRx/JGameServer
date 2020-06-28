package com.jacey.game.db.service.impl;

import com.jacey.game.db.dao.SessionIdDAO;
import com.jacey.game.db.service.SessionIdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Description: sessionId操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Service
public class SessionIdServiceImpl implements SessionIdService {
    
    @Autowired
    private SessionIdDAO sessionIdDAO;
    
    /**
     * 添加和获取下一个可用的sessionId(redis提供的自增id)
     * @return
     */
    @Override
    public int addAndGetNextAvailableSessionId() {
        return sessionIdDAO.addAndGetNextAvailableSessionId();
    }

    /**
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getOneUserIdToSessionId(int userId) {
        return sessionIdDAO.getOneUserIdToSessionId(userId);
    }

    /**
     *
     * @return
     */
    @Override
    public Map<Integer, Integer> getAllUserIdToSessionId() {
        return sessionIdDAO.getAllUserIdToSessionId();
    }

    /**
     *
     * @param userId
     * @param sessionId
     */
    @Override
    public void setOneUserIdToSessionId(int userId, int sessionId) {
        sessionIdDAO.setOneUserIdToSessionId(userId, sessionId);
    }

    /**
     *
     * @param userId
     */
    @Override
    public void removeOneUserIdToSessionId(int userId) {
        sessionIdDAO.removeOneUserIdToSessionId(userId);
    }

    /**
     *
     * @param sessionId
     * @return
     */
    @Override
    public Integer getOneSessionIdToUserId(int sessionId) {
        return sessionIdDAO.getOneSessionIdToUserId(sessionId);
    }

    /**
     *
     * @param sessionId
     * @param userId
     */
    @Override
    public void setOneSessionIdToUserId(int sessionId, int userId) {
        sessionIdDAO.setOneSessionIdToUserId(sessionId, userId);
    }

    /**
     *
     * @param sessionId
     */
    @Override
    public void removeOneSessionIdToUserId(int sessionId) {
        sessionIdDAO.removeOneSessionIdToUserId(sessionId);
    }

}
