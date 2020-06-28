package com.jacey.game.db.service;

import java.util.Map;

/**
 * @Description: 聊天服务器负载操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface ChatServerLoadBalanceService {

    Integer getOneBattleIdToChatServerId(String battleId);

    void setOneBattleIdToChatServerId(String battleId, int chatServerId);

    void removeOneBattleIdToChatServerId(String battleId);

    /**
     * 设置某个Chat聊天服务器负载情况
     * @param ChatServerId  	聊天服务器id
     * @param count				负载情况（连接数）
     */
    void setOneChatServerLoadBalance(int ChatServerId, int count);

    /**
     * 修改某个Chat聊天服务器的负载情况
     * @param ChatServerId 	聊天服务器id
     * @param changeCount		负载情况修改数值（连接数修改）
     */
    void changeOneChatServerLoadBalance(int ChatServerId, int changeCount);

    /**
     * 移除某个Chat聊天服务器负载情况
     * @param ChatServerId 	聊天服务器id
     */
    void removeOneChatServerLoadBalance(int ChatServerId);

    /**
     * 获取所有Chat聊天服务器的负载信息
     * @return
     */
    Map<Integer, Integer> getAllChatServerLoadBalance();

    /**
     * 清除Chat聊天服务器的所有负载信息
     */
    void cleanChatServerLoadBalance();

    /**
     * 获取空闲Chat聊天服务器id
     * @return
     */
    Integer getLeisureChatServerId();

    void setOneChatServerIdToAkkaPath(int ChatServerId, String akkaPath);

    String getOneChatServerIdToAkkaPath(int ChatServerId);

    Map<Integer, String> getAllChatServerIdToAkkaPath();

    void removeOneChatServerIdToAkkaPath(int ChatServerId);

    void cleanChatServerIdToAkkaPath();


}
