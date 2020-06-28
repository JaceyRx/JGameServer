package com.jacey.game.db.dao;

import com.jacey.game.db.constants.RedisKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Description: gateway 服务器负债 Redis操作DAO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Repository(value = "gatewayServerLoadBalanceDAO")
public class GatewayServerLoadBalanceDAO {

    @Autowired
    RedisTemplate jedisTemplate;

    /** 在线客户端所连的gatewayId（map类型，key:sessionId, value:gatewayId）*/
    @Resource(name = "integerTemplate")
    private HashOperations<String, String, Integer> sessionIdToGatewayIdOps;

    /** gateway服务器的负载（zset类型，score:服务器负载, value:gatewayId） */
    @Resource(name = "stringTemplate")
    private ZSetOperations<String, String> gatewayLoadBalanceOps;

    /** 已注册到GM服务器的gateway服务器id对应的akka地址（map类型，key:gatewayId, value:akka地址） */
    @Resource(name = "stringTemplate")
    private HashOperations<String, String, String> gatewayIdToAkkaPathOps;

    /** 已注册到GM服务器的gateway服务器id对应的供客户端连接的地址（map类型，key:gatewayId, value:供客户端连接的地址） */
    @Resource(name = "stringTemplate")
    private HashOperations<String, String, String> gatewayIdToConnectPathOps;

    public void setOneSessionIdToGatewayId(int userId, int gatewayId) {
        sessionIdToGatewayIdOps.put(RedisKeyHelper.getSessionIdToGatewayIdRedisKey(), String.valueOf(userId),
                gatewayId);
    }

    public Integer getOneSessionIdToGatewayId(int userId) {
        return sessionIdToGatewayIdOps.get(RedisKeyHelper.getSessionIdToGatewayIdRedisKey(), String.valueOf(userId));
    }

    public void removeOneSessionIdToGatewayId(int userId) {
        sessionIdToGatewayIdOps.delete(RedisKeyHelper.getSessionIdToGatewayIdRedisKey(), String.valueOf(userId));
    }

    public void setOneGatewayLoadBalance(int gatewayId, int count) {
        gatewayLoadBalanceOps.add(RedisKeyHelper.getGatewayLoadBalanceRedisKey(), String.valueOf(gatewayId), count);
    }

    public void changeOneGatewayLoadBalance(int gatewayId, int changeCount) {
        gatewayLoadBalanceOps.incrementScore(RedisKeyHelper.getGatewayLoadBalanceRedisKey(), String.valueOf(gatewayId),
                changeCount);
    }

    public void removeOneGatewayLoadBalance(int gatewayId) {
        gatewayLoadBalanceOps.remove(RedisKeyHelper.getGatewayLoadBalanceRedisKey(), String.valueOf(gatewayId));
    }

    public Map<Integer, Integer> getAllGatewayLoadBalance() {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        Set<ZSetOperations.TypedTuple<String>> set = gatewayLoadBalanceOps
                .rangeWithScores(RedisKeyHelper.getGatewayLoadBalanceRedisKey(), 0, -1);
        if (set != null) {
            for (ZSetOperations.TypedTuple<String> tuple : set) {
                result.put(Integer.parseInt(tuple.getValue()), tuple.getScore().intValue());
            }
        }
        return result;
    }

    public void cleanGatewayLoadBalance() {
        jedisTemplate.delete(RedisKeyHelper.getGatewayLoadBalanceRedisKey());
    }

    public Integer getLeisureGatewayId() {
        Set<String> serverSet = gatewayLoadBalanceOps.range(RedisKeyHelper.getGatewayLoadBalanceRedisKey(), 0, 0);
        if (serverSet != null && serverSet.isEmpty() == false) {
            return Integer.parseInt(serverSet.iterator().next());
        } else {
            return null;
        }
    }

    public void setOneGatewayIdToAkkaPath(int gatewayId, String akkaPath) {
        gatewayIdToAkkaPathOps.put(RedisKeyHelper.getGatewayIdToAkkaPathRedisKey(), String.valueOf(gatewayId),
                akkaPath);
    }

    public String getOneGatewayIdToAkkaPath(int gatewayId) {
        return gatewayIdToAkkaPathOps.get(RedisKeyHelper.getGatewayIdToAkkaPathRedisKey(), String.valueOf(gatewayId));
    }

    public Map<Integer, String> getAllGatewayIdToAkkaPath() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        Map<String, String> map = gatewayIdToAkkaPathOps.entries(RedisKeyHelper.getGatewayIdToAkkaPathRedisKey());
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                result.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    public void removeOneGatewayIdToAkkaPath(int gatewayId) {
        gatewayIdToAkkaPathOps.delete(RedisKeyHelper.getGatewayIdToAkkaPathRedisKey(), String.valueOf(gatewayId));
    }

    public void cleanGatewayIdToAkkaPath() {
        jedisTemplate.delete(RedisKeyHelper.getGatewayIdToAkkaPathRedisKey());
    }

    public void setOneGatewayIdToConnectPath(int gatewayId, String connectPath) {
        gatewayIdToConnectPathOps.put(RedisKeyHelper.getGatewayIdToConnectPathRedisKey(), String.valueOf(gatewayId),
                connectPath);
    }

    public String getOneGatewayIdToConnectPath(int gatewayId) {
        return gatewayIdToConnectPathOps.get(RedisKeyHelper.getGatewayIdToConnectPathRedisKey(),
                String.valueOf(gatewayId));
    }

    public void removeOneGatewayIdToConnectPath(int gatewayId) {
        gatewayIdToConnectPathOps.delete(RedisKeyHelper.getGatewayIdToConnectPathRedisKey(), String.valueOf(gatewayId));
    }

    public void cleanGatewayIdToConnectPath() {
        jedisTemplate.delete(RedisKeyHelper.getGatewayIdToConnectPathRedisKey());
    }


}
