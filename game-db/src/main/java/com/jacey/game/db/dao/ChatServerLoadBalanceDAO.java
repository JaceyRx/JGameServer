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
 * @Description: chat 服务器负债 Redis操作DAO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Repository(value = "chatServerLoadBalanceDAO")
public class ChatServerLoadBalanceDAO {

    @Autowired
    RedisTemplate jedisTemplate;

    /** 进行中的battleId与处理这场战斗聊天的chatServerId的对应关系（map类型，key:battleId, value:chatServerId） */
    @Resource(name = "integerTemplate")
    private HashOperations<String, String, Integer> battleIdToChatServerIdOps;

    /** chat服务器的负载（zset类型，score:服务器负载, value:chatServerId） */
    @Resource(name = "stringTemplate")
    private ZSetOperations<String, String> chatServerLoadBalanceOps;

    /** 已注册到GM服务器的Chat服务器id对应的akka地址（map类型，key:ChatServerId, value:akka地址） */
    @Resource(name = "stringTemplate")
    private HashOperations<String, String, String> chatServerIdToAkkaPathOps;

    /** battleId与ChatServerId绑定 */
    public Integer getOneBattleIdToChatServerId(String battleId) {
        return battleIdToChatServerIdOps.get(RedisKeyHelper.getBattleIdToChatServerIdRedisKey(), battleId);
    }

    public void setOneBattleIdToChatServerId(String battleId, int chatServerId) {
        battleIdToChatServerIdOps.put(RedisKeyHelper.getBattleIdToChatServerIdRedisKey(), battleId, chatServerId);
    }

    public void removeOneBattleIdToChatServerId(String battleId) {
        battleIdToChatServerIdOps.delete(RedisKeyHelper.getBattleIdToChatServerIdRedisKey(), battleId);
    }

    /**
     * 设置单个chatServerId 与 AkkaPath
     * @param chatServerId	聊天服务器ID
     * @param akkaPath
     */
    public void setOneChatServerIdToAkkaPath(int chatServerId, String akkaPath) {
        chatServerIdToAkkaPathOps.put(RedisKeyHelper.getChatServerIdToAkkaPathRedisKey(),
                String.valueOf(chatServerId), akkaPath);
    }

    /**
     * 根据 chatServerId获取单个 AkkaPath
     * @param chatServerId	聊天服务器ID
     * @return
     */
    public String getOneChatServerIdToAkkaPath(int chatServerId) {
        return chatServerIdToAkkaPathOps.get(RedisKeyHelper.getChatServerIdToAkkaPathRedisKey(),
                String.valueOf(chatServerId));
    }

    /**
     * 获取所有ChatServer akka Path
     * @return
     */
    public Map<Integer, String> getAllChatServerIdToAkkaPath() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        Map<String, String> map = chatServerIdToAkkaPathOps
                .entries(RedisKeyHelper.getChatServerIdToAkkaPathRedisKey());
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                result.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 根据ChatServerId删除单个AkkaPath
     * @param chatServerId
     */
    public void removeOneChatServerIdToAkkaPath(int chatServerId) {
        chatServerIdToAkkaPathOps.delete(RedisKeyHelper.getChatServerIdToAkkaPathRedisKey(),
                String.valueOf(chatServerId));
    }

    /**
     * 清除所有：key chatServerIdToAkkaPath 下的数据
     */
    @SuppressWarnings("unchecked")
    public void cleanChatServerIdToAkkaPath() {
        jedisTemplate.delete(RedisKeyHelper.getChatServerIdToAkkaPathRedisKey());
    }

    /**
     * 设置chatServer负载
     * @param chatServerId
     * @param count
     */
    public void setOneChatServerLoadBalance(int chatServerId, int count) {
        chatServerLoadBalanceOps.add(RedisKeyHelper.getChatServerLoadBalanceRedisKey(),
                String.valueOf(chatServerId), count);
    }

    /**
     * 修改chatServer负载
     * @param chatServerId
     * @param changeCount
     */
    public void changeOneChatServerLoadBalance(int chatServerId, int changeCount) {
        chatServerLoadBalanceOps.incrementScore(RedisKeyHelper.getChatServerLoadBalanceRedisKey(),
                String.valueOf(chatServerId), changeCount);
    }

    /**
     * 移除chatServer 负载
     * @param chatServerId
     */
    public void removeOneChatServerLoadBalance(int chatServerId) {
        chatServerLoadBalanceOps.remove(RedisKeyHelper.getChatServerLoadBalanceRedisKey(),
                String.valueOf(chatServerId));
    }

    /**
     * 获取所有ChatServer 负载信息
     * @return
     */
    public Map<Integer, Integer> getAllChatServerLoadBalance() {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        Set<ZSetOperations.TypedTuple<String>> set = chatServerLoadBalanceOps
                .rangeWithScores(RedisKeyHelper.getChatServerLoadBalanceRedisKey(), 0, -1);
        if (set != null) {
            for (ZSetOperations.TypedTuple<String> tuple : set) {
                result.put(Integer.parseInt(tuple.getValue()), tuple.getScore().intValue());
            }
        }
        return result;
    }

    /**
     * 清除所有ChatServer负载
     */
    @SuppressWarnings("unchecked")
    public void cleanChatServerLoadBalance() {
        jedisTemplate.delete(RedisKeyHelper.getChatServerLoadBalanceRedisKey());
    }

    /**
     * 获取空闲的chatServer Id
     * @return
     */
    public Integer getLeisureChatServerId() {
        Set<String> serverSet = chatServerLoadBalanceOps.range(RedisKeyHelper.getChatServerLoadBalanceRedisKey(), 0,
                0);
        if (serverSet != null && serverSet.isEmpty() == false) {
            return Integer.parseInt(serverSet.iterator().next());
        } else {
            return null;
        }
    }
}
