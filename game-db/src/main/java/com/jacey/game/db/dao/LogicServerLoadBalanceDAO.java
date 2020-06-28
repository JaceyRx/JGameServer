package com.jacey.game.db.dao;

import com.jacey.game.db.constants.RedisKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Description: 逻辑服务器负载相关操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Repository(value = "logicServerLoadBalanceDAO")
public class LogicServerLoadBalanceDAO {

    @Autowired
    RedisTemplate jedisTemplate;

    /** 在线客户端对应UserActor所在的logic服务器（map类型，key:sessionId, value:logicServerId） */
    @Resource(name = "integerTemplate")
    private HashOperations<String, String, Integer> sessionIdToLogicServerIdOps;

    /** logic服务器的负载（zset类型，score:服务器负载, value:logicServerId） */
    @Resource(name = "stringTemplate")
    private ZSetOperations<String, String> logicServerLoadBalanceOps;

    /** 已注册到GM服务器的logic服务器id对应的akka地址（map类型，key:logicServerId, value:akka地址） */
    @Resource(name = "stringTemplate")
    private HashOperations<String, String, String> logicServerIdToAkkaPathOps;

    /** 已注册到GM服务器的主logic服务器id（value类型，mainLogicServerId） */
    @Resource(name = "integerTemplate")
    private ValueOperations<String, Integer> mainLogicServerIdOps;

    public void setOneSessionIdToLogicServerId(int userId, int logicServerId) {
        sessionIdToLogicServerIdOps.put(RedisKeyHelper.getSessionIdToLogicServerIdRedisKey(), String.valueOf(userId),
                logicServerId);
    }

    public Integer getOneSessionIdToLogicServerId(int userId) {
        return sessionIdToLogicServerIdOps.get(RedisKeyHelper.getSessionIdToLogicServerIdRedisKey(),
                String.valueOf(userId));
    }

    public void removeOneSessionIdToLogicServerId(int userId) {
        sessionIdToLogicServerIdOps.delete(RedisKeyHelper.getSessionIdToLogicServerIdRedisKey(),
                String.valueOf(userId));
    }

    public void setOneLogicServerLoadBalance(int logicServerId, int count) {
        logicServerLoadBalanceOps.add(RedisKeyHelper.getLogicServerLoadBalanceRedisKey(), String.valueOf(logicServerId),
                count);
    }

    public void changeOneLogicServerLoadBalance(int logicServerId, int changeCount) {
        logicServerLoadBalanceOps.incrementScore(RedisKeyHelper.getLogicServerLoadBalanceRedisKey(),
                String.valueOf(logicServerId), changeCount);
    }

    public void removeOneLogicServerLoadBalance(int logicServerId) {
        logicServerLoadBalanceOps.remove(RedisKeyHelper.getLogicServerLoadBalanceRedisKey(),
                String.valueOf(logicServerId));
    }

    /**
     * 获取所有逻辑服务器的负载信息
     * @return
     */
    public Map<Integer, Integer> getAllLogicServerLoadBalance() {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        Set<ZSetOperations.TypedTuple<String>> set = logicServerLoadBalanceOps
                .rangeWithScores(RedisKeyHelper.getLogicServerLoadBalanceRedisKey(), 0, -1);
        if (set != null) {
            for (ZSetOperations.TypedTuple<String> tuple : set) {
                // 获取服务器id与排序字段（负载数）
                result.put(Integer.parseInt(tuple.getValue()), tuple.getScore().intValue());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void cleanLogicServerLoadBalance() {
        jedisTemplate.delete(RedisKeyHelper.getLogicServerLoadBalanceRedisKey());
    }

    public Integer getLeisureLogicServerId() {
        Set<String> serverSet = logicServerLoadBalanceOps.range(RedisKeyHelper.getLogicServerLoadBalanceRedisKey(), 0,
                0);
        if (serverSet != null && serverSet.isEmpty() == false) {
            return Integer.parseInt(serverSet.iterator().next());
        } else {
            return null;
        }
    }

    public void setOneLogicServerIdToAkkaPath(int logicServerId, String akkaPath) {
        logicServerIdToAkkaPathOps.put(RedisKeyHelper.getLogicServerIdToAkkaPathRedisKey(),
                String.valueOf(logicServerId), akkaPath);
    }

    public String getOneLogicServerIdToAkkaPath(int logicServerId) {
        return logicServerIdToAkkaPathOps.get(RedisKeyHelper.getLogicServerIdToAkkaPathRedisKey(),
                String.valueOf(logicServerId));
    }

    public Map<Integer, String> getAllLogicServerIdToAkkaPath() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        Map<String, String> map = logicServerIdToAkkaPathOps
                .entries(RedisKeyHelper.getLogicServerIdToAkkaPathRedisKey());
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                result.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    public void removeOneLogicServerIdToAkkaPath(int logicServerId) {
        logicServerIdToAkkaPathOps.delete(RedisKeyHelper.getLogicServerIdToAkkaPathRedisKey(),
                String.valueOf(logicServerId));
    }

    @SuppressWarnings("unchecked")
    public void cleanLogicServerIdToAkkaPath() {
        jedisTemplate.delete(RedisKeyHelper.getLogicServerIdToAkkaPathRedisKey());
    }

    public void setMainLogicServerId(int mainLogicServerId) {
        mainLogicServerIdOps.set(RedisKeyHelper.getMainLogicServerIdRedisKey(), mainLogicServerId);
    }

    public int getMainLogicServerId() {
        Integer result = mainLogicServerIdOps.get(RedisKeyHelper.getMainLogicServerIdRedisKey());
        return result == null ? 0 : result;
    }
}
