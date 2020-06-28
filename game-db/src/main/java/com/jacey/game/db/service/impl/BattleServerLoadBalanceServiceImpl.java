package com.jacey.game.db.service.impl;

import com.jacey.game.db.dao.BattleServerLoadBalanceDAO;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Description: 对战服务器负载操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Service
public class BattleServerLoadBalanceServiceImpl implements BattleServerLoadBalanceService {

    @Autowired
    private BattleServerLoadBalanceDAO loadBalanceDAO;

    @Override
    public void setBattleUserIdToBattleId(int userId, String battleId) {
        loadBalanceDAO.setBattleUserIdToBattleId(userId, battleId);
    }

    @Override
    public String getBattleUserIdToBattleId(int userId) {
        return loadBalanceDAO.getBattleUserIdToBattleId(userId);
    }

    @Override
    public void removeBattleUserIdToBattleId(int userId) {
        loadBalanceDAO.removeBattleUserIdToBattleId(userId);
    }

    @Override
    public Integer getOneBattleIdToBattleServerId(String battleId) {
        return loadBalanceDAO.getOneBattleIdToBattleServerId(battleId);
    }

    @Override
    public void setOneBattleIdToBattleServerId(String battleId, int battleServerId) {
        loadBalanceDAO.setOneBattleIdToBattleServerId(battleId, battleServerId);
    }

    @Override
    public void removeOneBattleIdToBattleServerId(String battleId) {
        loadBalanceDAO.removeOneBattleIdToBattleServerId(battleId);
    }

    /**
     * 设置某个Battle对战服务器负载情况
     * @param battleServerId  	对战服务器id
     * @param count				负载情况（连接数）
     */
    @Override
    public void setOneBattleServerLoadBalance(int battleServerId, int count) {
        loadBalanceDAO.setOneBattleServerLoadBalance(battleServerId, count);
    }

    /**
     * 修改某个Battle对战服务器的负载情况
     * @param battleServerId 	对战服务器id
     * @param changeCount		负载情况修改数值（连接数修改）
     */
    @Override
    public void changeOneBattleServerLoadBalance(int battleServerId, int changeCount) {
        loadBalanceDAO.changeOneBattleServerLoadBalance(battleServerId, changeCount);
    }

    /**
     * 移除某个Battle对战服务器负载情况
     * @param battleServerId 	对战服务器id
     */
    @Override
    public void removeOneBattleServerLoadBalance(int battleServerId) {
        loadBalanceDAO.removeOneBattleServerLoadBalance(battleServerId);
    }

    /**
     * 获取所有Battle对战服务器的负载信息
     * @return
     */
    @Override
    public Map<Integer, Integer> getAllBattleServerLoadBalance() {
        return loadBalanceDAO.getAllBattleServerLoadBalance();
    }

    /**
     * 清除Battle对战服务器的所有负载信息
     */
    @Override
    public void cleanBattleServerLoadBalance() {
        loadBalanceDAO.cleanBattleServerLoadBalance();
    }

    /**
     * 获取Battle空闲Battle对战服务器id
     * @return
     */
    @Override
    public Integer getLeisureBattleServerId() {
        return loadBalanceDAO.getLeisureBattleServerId();
    }

    @Override
    public void setOneBattleServerIdToAkkaPath(int battleServerId, String akkaPath) {
        loadBalanceDAO.setOneBattleServerIdToAkkaPath(battleServerId, akkaPath);
    }

    @Override
    public String getOneBattleServerIdToAkkaPath(int battleServerId) {
        return loadBalanceDAO.getOneBattleServerIdToAkkaPath(battleServerId);
    }

    @Override
    public Map<Integer, String> getAllBattleServerIdToAkkaPath() {
        return loadBalanceDAO.getAllBattleServerIdToAkkaPath();
    }

    @Override
    public void removeOneBattleServerIdToAkkaPath(int battleServerId) {
        loadBalanceDAO.removeOneBattleServerIdToAkkaPath(battleServerId);
    }

    @Override
    public void cleanBattleServerIdToAkkaPath() {
        loadBalanceDAO.cleanBattleServerIdToAkkaPath();
    }

}
