package com.jacey.game.db.service;

import java.util.Map;

/**
 * @Description: logic服务器负债操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface LogicServerLoadBalanceService {


    /**
     * 设置sessionId与逻辑服务器id 绑定
     * @param sessionId
     * @param logicServerId
     */
    public void setOneSessionIdToLogicServerId(int sessionId, int logicServerId);

    /**
     * 获取sessionId所绑定的逻辑服务器id
     * @param sessionId
     * @return
     */
    public Integer getOneSessionIdToLogicServerId(int sessionId);

    /**
     *
     * @param sessionId
     */
    public void removeOneSessionIdToLogicServerId(int sessionId);

    /**
     * 设置某个Logic逻辑服务器负载情况
     * @param logicServerId  逻辑服务器id
     * @param count			 负载情况（连接数）
     */
    void setOneLogicServerLoadBalance(int logicServerId, int count);

    /**
     * 修改某个Logic逻辑服务器的负载情况
     * @param logicServerId   逻辑服务器id
     * @param changeCount	  负载情况修改数值（连接数修改）
     */
    void changeOneLogicServerLoadBalance(int logicServerId, int changeCount);

    /**
     * 移除某个Logic逻辑服务器负载情况
     * @param logicServerId   逻辑服务器id
     */
    public void removeOneLogicServerLoadBalance(int logicServerId);

    /**
     * 获取所有Logic逻辑服务器的负载信息
     * @return
     */
    Map<Integer, Integer> getAllLogicServerLoadBalance();

    /**
     * 清除Logic逻辑服务器的所有负载信息
     */
    void cleanLogicServerLoadBalance();

    /**
     * 获取空闲Logic逻辑服务器id
     * @return
     */
    Integer getLeisureLogicServerId();

    /**
     * 设置Logic逻辑服务器id对应的AkkaPath
     * @param logicServerId		逻辑服务器id
     * @param akkaPath			AkkaPath
     */
    void setOneLogicServerIdToAkkaPath(int logicServerId, String akkaPath);

    /**
     * 获取Logic逻辑服务器id对应的AkkaPath
     * @param logicServerId
     * @return
     */
    String getOneLogicServerIdToAkkaPath(int logicServerId);

    /**
     * 获取所有Logic逻辑服务器对应的AkkaPath
     * @return
     */
    Map<Integer, String> getAllLogicServerIdToAkkaPath();

    /**
     * 移除某个Logic逻辑服务器对应的AkkaPath
     * @param logicServerId
     */
    void removeOneLogicServerIdToAkkaPath(int logicServerId);

    /**
     * 清除所有Logic逻辑服务器AkkaPath
     */
    void cleanLogicServerIdToAkkaPath();

    /**
     * 设置主Logic逻辑服务器id
     * @param mainLogicServerId
     */
    void setMainLogicServerId(int mainLogicServerId);

    /** 获取Logic逻辑服务器id */
    int getMainLogicServerId();

}
