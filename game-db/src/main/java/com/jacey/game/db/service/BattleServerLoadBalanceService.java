package com.jacey.game.db.service;

import java.util.Map;

/**
 * @Description: 对战服务器负债操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface BattleServerLoadBalanceService {

    void setBattleUserIdToBattleId(int userId, String battleId);

    String getBattleUserIdToBattleId(int userId);

    void removeBattleUserIdToBattleId(int userId);

    Integer getOneBattleIdToBattleServerId(String battleId);

    void setOneBattleIdToBattleServerId(String battleId, int battleServerId);

    void removeOneBattleIdToBattleServerId(String battleId);

    /**
     * 设置某个Battle对战服务器负载情况
     * @param battleServerId  	对战服务器id
     * @param count				负载情况（连接数）
     */
    void setOneBattleServerLoadBalance(int battleServerId, int count);

    /**
     * 修改某个Battle对战服务器的负载情况
     * @param battleServerId 	对战服务器id
     * @param changeCount		负载情况修改数值（连接数修改）
     */
    void changeOneBattleServerLoadBalance(int battleServerId, int changeCount);

    /**
     * 移除某个Battle对战服务器负载情况
     * @param battleServerId 	对战服务器id
     */
    void removeOneBattleServerLoadBalance(int battleServerId);

    /**
     * 获取所有Battle对战服务器的负载信息
     * @return
     */
    Map<Integer, Integer> getAllBattleServerLoadBalance();

    /**
     * 清除Battle对战服务器的所有负载信息
     */
    void cleanBattleServerLoadBalance();

    /**
     * 获取Battle空闲Battle对战服务器id
     * @return
     */
    Integer getLeisureBattleServerId();

    void setOneBattleServerIdToAkkaPath(int battleServerId, String akkaPath);

    String getOneBattleServerIdToAkkaPath(int battleServerId);

    Map<Integer, String> getAllBattleServerIdToAkkaPath();

    void removeOneBattleServerIdToAkkaPath(int battleServerId);

    void cleanBattleServerIdToAkkaPath();





}
