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
 * @Description: battle 服务器负债 Redis操作DAO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Repository(value = "battleServerLoadBalanceDAO")
public class BattleServerLoadBalanceDAO {

    @Autowired
    RedisTemplate jedisTemplate;

    /** 对战中的玩家userId与battleId的对应关系（map类型，key:userId, value:battleId） */
    @Resource(name = "stringTemplate")
    private HashOperations<String, String, String> battleUserIdToBattleIdOps;

    /** 进行中的battleId与处理这场战斗的battleServerId的对应关系（map类型，key:battleId, value:battleServerId） */
    @Resource(name = "integerTemplate")
    private HashOperations<String, String, Integer> battleIdToBattleServerIdOps;

    /** battle服务器的负载（zset类型，score:服务器负载, value:battleServerId） */
    @Resource(name = "stringTemplate")
    private ZSetOperations<String, String> battleServerLoadBalanceOps;

    /** 已注册到GM服务器的battle服务器id对应的akka地址（map类型，key:battleServerId, value:akka地址） */
    @Resource(name = "stringTemplate")
    private HashOperations<String, String, String> battleServerIdToAkkaPathOps;


    public void setBattleUserIdToBattleId(int userId, String battleId) {
        battleUserIdToBattleIdOps.put(RedisKeyHelper.getBattleUserIdToBattleIdRedisKey(), String.valueOf(userId),
                battleId);
    }

    public String getBattleUserIdToBattleId(int userId) {
        return battleUserIdToBattleIdOps.get(RedisKeyHelper.getBattleUserIdToBattleIdRedisKey(),
                String.valueOf(userId));
    }

    public void removeBattleUserIdToBattleId(int userId) {
        battleUserIdToBattleIdOps.delete(RedisKeyHelper.getBattleUserIdToBattleIdRedisKey(), String.valueOf(userId));
    }

    public Integer getOneBattleIdToBattleServerId(String battleId) {
        return battleIdToBattleServerIdOps.get(RedisKeyHelper.getBattleIdToBattleServerIdRedisKey(), battleId);
    }

    public void setOneBattleIdToBattleServerId(String battleId, int battleServerId) {
        battleIdToBattleServerIdOps.put(RedisKeyHelper.getBattleIdToBattleServerIdRedisKey(), battleId, battleServerId);
    }

    public void removeOneBattleIdToBattleServerId(String battleId) {
        battleIdToBattleServerIdOps.delete(RedisKeyHelper.getBattleIdToBattleServerIdRedisKey(), battleId);
    }

    public void setOneBattleServerLoadBalance(int battleServerId, int count) {
        battleServerLoadBalanceOps.add(RedisKeyHelper.getBattleServerLoadBalanceRedisKey(),
                String.valueOf(battleServerId), count);
    }

    public void changeOneBattleServerLoadBalance(int battleServerId, int changeCount) {
        battleServerLoadBalanceOps.incrementScore(RedisKeyHelper.getBattleServerLoadBalanceRedisKey(),
                String.valueOf(battleServerId), changeCount);
    }

    public void removeOneBattleServerLoadBalance(int battleServerId) {
        battleServerLoadBalanceOps.remove(RedisKeyHelper.getBattleServerLoadBalanceRedisKey(),
                String.valueOf(battleServerId));
    }

    public Map<Integer, Integer> getAllBattleServerLoadBalance() {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        Set<ZSetOperations.TypedTuple<String>> set = battleServerLoadBalanceOps
                .rangeWithScores(RedisKeyHelper.getBattleServerLoadBalanceRedisKey(), 0, -1);
        if (set != null) {
            for (ZSetOperations.TypedTuple<String> tuple : set) {
                result.put(Integer.parseInt(tuple.getValue()), tuple.getScore().intValue());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void cleanBattleServerLoadBalance() {
        jedisTemplate.delete(RedisKeyHelper.getBattleServerLoadBalanceRedisKey());
    }

    public Integer getLeisureBattleServerId() {
        Set<String> serverSet = battleServerLoadBalanceOps.range(RedisKeyHelper.getBattleServerLoadBalanceRedisKey(), 0,
                0);
        if (serverSet != null && serverSet.isEmpty() == false) {
            return Integer.parseInt(serverSet.iterator().next());
        } else {
            return null;
        }
    }


    public void setOneBattleServerIdToAkkaPath(int battleServerId, String akkaPath) {
        battleServerIdToAkkaPathOps.put(RedisKeyHelper.getBattleServerIdToAkkaPathRedisKey(),
                String.valueOf(battleServerId), akkaPath);
    }

    public String getOneBattleServerIdToAkkaPath(int battleServerId) {
        return battleServerIdToAkkaPathOps.get(RedisKeyHelper.getBattleServerIdToAkkaPathRedisKey(),
                String.valueOf(battleServerId));
    }

    public Map<Integer, String> getAllBattleServerIdToAkkaPath() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        Map<String, String> map = battleServerIdToAkkaPathOps
                .entries(RedisKeyHelper.getBattleServerIdToAkkaPathRedisKey());
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                result.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    public void removeOneBattleServerIdToAkkaPath(int battleServerId) {
        battleServerIdToAkkaPathOps.delete(RedisKeyHelper.getBattleServerIdToAkkaPathRedisKey(),
                String.valueOf(battleServerId));
    }

    @SuppressWarnings("unchecked")
    public void cleanBattleServerIdToAkkaPath() {
        jedisTemplate.delete(RedisKeyHelper.getBattleServerIdToAkkaPathRedisKey());
    }

}
