package com.jacey.game.db.dao;

import com.jacey.game.db.constants.RedisKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: SessionId Redis 操作DAO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Repository(value = "sessionIdDAO")
public class SessionIdDAO {

    @SuppressWarnings("rawtypes")
    @Autowired
    RedisTemplate jedisTemplate;

    /**	客户端自增的sessionId（value类型，自增的sessionId）*/
    @Resource(name = "stringTemplate")
    private ValueOperations<String, String> sessionIdAutoIncreaseOps;


    /** 已登录成功玩家userId与sessionId的对应关系（map类型，key:userId, value:sessionId）*/
    @Resource(name = "integerTemplate")
    private HashOperations<String, String, Integer> userIdToSessionIdOps;

    /** 已登录成功玩家sessionId与userId的对应关系（map类型，key:sessionId, value:userId） */
    @Resource(name = "integerTemplate")
    private HashOperations<String, String, Integer> sessionIdToUserIdOps;



    public int addAndGetNextAvailableSessionId() {
        return sessionIdAutoIncreaseOps.increment(RedisKeyHelper.getSessionIdAutoIncreaseRedisKey(), 1).intValue();
    }


    public Integer getOneUserIdToSessionId(int userId) {
        return userIdToSessionIdOps.get(RedisKeyHelper.getUserIdToSessionIdRedisKey(), String.valueOf(userId));
    }

    public Map<Integer, Integer> getAllUserIdToSessionId() {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        Map<String, Integer> map = userIdToSessionIdOps.entries(RedisKeyHelper.getUserIdToSessionIdRedisKey());
        if (map != null) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                result.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    public void setOneUserIdToSessionId(int userId, int sessionId) {
        userIdToSessionIdOps.put(RedisKeyHelper.getUserIdToSessionIdRedisKey(), String.valueOf(userId), sessionId);
    }

    public void removeOneUserIdToSessionId(int userId) {
        userIdToSessionIdOps.delete(RedisKeyHelper.getUserIdToSessionIdRedisKey(), String.valueOf(userId));
    }

    public Integer getOneSessionIdToUserId(int sessionId) {
        return sessionIdToUserIdOps.get(RedisKeyHelper.getSessionIdToUserIdRedisKey(), String.valueOf(sessionId));
    }

    public void setOneSessionIdToUserId(int sessionId, int userId) {
        sessionIdToUserIdOps.put(RedisKeyHelper.getSessionIdToUserIdRedisKey(), String.valueOf(sessionId), userId);
    }

    public void removeOneSessionIdToUserId(int sessionId) {
        sessionIdToUserIdOps.delete(RedisKeyHelper.getSessionIdToUserIdRedisKey(), String.valueOf(sessionId));
    }

}
