package com.jacey.game.db.service.impl;

import com.jacey.game.db.dao.ChatServerLoadBalanceDAO;
import com.jacey.game.db.service.ChatServerLoadBalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Description: 聊天服务器负载操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Service
public class ChatServerLoadBalanceServiceImpl implements ChatServerLoadBalanceService {

    @Autowired
    private ChatServerLoadBalanceDAO loadBalanceDAO;

    @Override
    public Integer getOneBattleIdToChatServerId(String battleId) {
        return loadBalanceDAO.getOneBattleIdToChatServerId(battleId);
    }

    @Override
    public void setOneBattleIdToChatServerId(String battleId, int chatServerId) {
        loadBalanceDAO.setOneBattleIdToChatServerId(battleId, chatServerId);
    }

    @Override
    public void removeOneBattleIdToChatServerId(String battleId) {
        loadBalanceDAO.removeOneBattleIdToChatServerId(battleId);
    }

    /**
     * 设置某个Chat聊天服务器负载情况
     * @param ChatServerId  	聊天服务器id
     * @param count				负载情况（连接数）
     */
    @Override
    public void setOneChatServerLoadBalance(int ChatServerId, int count) {
        loadBalanceDAO.setOneChatServerLoadBalance(ChatServerId, count);
    }

    /**
     * 修改某个Chat聊天服务器的负载情况
     * @param ChatServerId 	聊天服务器id
     * @param changeCount		负载情况修改数值（连接数修改）
     */
    @Override
    public void changeOneChatServerLoadBalance(int ChatServerId, int changeCount) {
        loadBalanceDAO.changeOneChatServerLoadBalance(ChatServerId, changeCount);
    }

    /**
     * 移除某个Chat聊天服务器负载情况
     * @param ChatServerId 	聊天服务器id
     */
    @Override
    public void removeOneChatServerLoadBalance(int ChatServerId) {
        loadBalanceDAO.removeOneChatServerLoadBalance(ChatServerId);
    }

    /**
     * 获取所有Chat聊天服务器的负载信息
     * @return
     */
    @Override
    public Map<Integer, Integer> getAllChatServerLoadBalance() {
        return loadBalanceDAO.getAllChatServerLoadBalance();
    }

    /**
     * 清除Chat聊天服务器的所有负载信息
     */
    @Override
    public void cleanChatServerLoadBalance() {
        loadBalanceDAO.cleanChatServerLoadBalance();
    }

    /**
     * 获取空闲Chat聊天服务器id
     * @return
     */
    @Override
    public Integer getLeisureChatServerId() {
        return loadBalanceDAO.getLeisureChatServerId();
    }

    @Override
    public void setOneChatServerIdToAkkaPath(int ChatServerId, String akkaPath) {
        loadBalanceDAO.setOneChatServerIdToAkkaPath(ChatServerId, akkaPath);
    }

    @Override
    public String getOneChatServerIdToAkkaPath(int ChatServerId) {
        return loadBalanceDAO.getOneChatServerIdToAkkaPath(ChatServerId);
    }

    @Override
    public Map<Integer, String> getAllChatServerIdToAkkaPath() {
        return loadBalanceDAO.getAllChatServerIdToAkkaPath();
    }

    @Override
    public void removeOneChatServerIdToAkkaPath(int ChatServerId) {
        loadBalanceDAO.removeOneChatServerIdToAkkaPath(ChatServerId);
    }

    @Override
    public void cleanChatServerIdToAkkaPath() {
        loadBalanceDAO.cleanChatServerIdToAkkaPath();
    }

}
