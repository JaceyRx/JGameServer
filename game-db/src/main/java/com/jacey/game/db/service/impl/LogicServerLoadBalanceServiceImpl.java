package com.jacey.game.db.service.impl;

import com.jacey.game.db.dao.LogicServerLoadBalanceDAO;
import com.jacey.game.db.service.LogicServerLoadBalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Description: logic服务器负债操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Service
public class LogicServerLoadBalanceServiceImpl implements LogicServerLoadBalanceService {

    @Autowired
    private LogicServerLoadBalanceDAO loadBalanceDAO;

    /**
     * 设置sessionId与逻辑服务器id 绑定
     * @param sessionId
     * @param logicServerId
     */
    @Override
    public void setOneSessionIdToLogicServerId(int sessionId, int logicServerId) {
        loadBalanceDAO.setOneSessionIdToLogicServerId(sessionId, logicServerId);
    }

    /**
     * 获取sessionId所绑定的逻辑服务器id
     * @param sessionId
     * @return
     */
    @Override
    public Integer getOneSessionIdToLogicServerId(int sessionId) {
        return loadBalanceDAO.getOneSessionIdToLogicServerId(sessionId);
    }


    @Override
    public void removeOneSessionIdToLogicServerId(int sessionId) {
        loadBalanceDAO.removeOneSessionIdToLogicServerId(sessionId);
    }


    /**
     * 设置某个Logic逻辑服务器负载情况
     * @param logicServerId  逻辑服务器id
     * @param count			 负载情况（连接数）
     */
    @Override
    public void setOneLogicServerLoadBalance(int logicServerId, int count) {
        loadBalanceDAO.setOneLogicServerLoadBalance(logicServerId, count);
    }

    /**
     * 修改某个Logic逻辑服务器的负载情况
     * @param logicServerId   逻辑服务器id
     * @param changeCount	  负载情况修改数值（连接数修改）
     */
    @Override
    public void changeOneLogicServerLoadBalance(int logicServerId, int changeCount) {
        loadBalanceDAO.changeOneLogicServerLoadBalance(logicServerId, changeCount);
    }

    /**
     * 移除某个Logic逻辑服务器负载情况
     * @param logicServerId   逻辑服务器id
     */
    @Override
    public void removeOneLogicServerLoadBalance(int logicServerId) {
        loadBalanceDAO.removeOneLogicServerLoadBalance(logicServerId);
    }

    /**
     * 获取所有Logic逻辑服务器的负载信息
     * @return
     */
    @Override
    public Map<Integer, Integer> getAllLogicServerLoadBalance() {
        return loadBalanceDAO.getAllLogicServerLoadBalance();
    }

    /**
     * 清除Logic逻辑服务器的所有负载信息
     */
    @Override
    public void cleanLogicServerLoadBalance() {
        loadBalanceDAO.cleanLogicServerLoadBalance();
    }

    /**
     * 获取空闲Logic逻辑服务器id
     * @return
     */
    @Override
    public Integer getLeisureLogicServerId() {
        return loadBalanceDAO.getLeisureLogicServerId();
    }

    /**
     * 设置Logic逻辑服务器id对应的AkkaPath
     * @param logicServerId		逻辑服务器id
     * @param akkaPath			AkkaPath
     */
    @Override
    public void setOneLogicServerIdToAkkaPath(int logicServerId, String akkaPath) {
        loadBalanceDAO.setOneLogicServerIdToAkkaPath(logicServerId, akkaPath);
    }

    /**
     * 获取Logic逻辑服务器id对应的AkkaPath
     * @param logicServerId
     * @return
     */
    @Override
    public String getOneLogicServerIdToAkkaPath(int logicServerId) {
        return loadBalanceDAO.getOneLogicServerIdToAkkaPath(logicServerId);
    }

    /**
     * 获取所有Logic逻辑服务器对应的AkkaPath
     * @return
     */
    @Override
    public Map<Integer, String> getAllLogicServerIdToAkkaPath() {
        return loadBalanceDAO.getAllLogicServerIdToAkkaPath();
    }

    /**
     * 移除某个Logic逻辑服务器对应的AkkaPath
     * @param logicServerId
     */
    @Override
    public void removeOneLogicServerIdToAkkaPath(int logicServerId) {
        loadBalanceDAO.removeOneLogicServerIdToAkkaPath(logicServerId);
    }

    /**
     * 清除所有Logic逻辑服务器AkkaPath
     */
    @Override
    public void cleanLogicServerIdToAkkaPath() {
        loadBalanceDAO.cleanLogicServerIdToAkkaPath();
    }

    /**
     * 设置主Logic逻辑服务器id
     * @param mainLogicServerId
     */
    @Override
    public void setMainLogicServerId(int mainLogicServerId) {
        loadBalanceDAO.setMainLogicServerId(mainLogicServerId);
    }

    /**
     * 获取Logic逻辑服务器id
     * @return
     */
    @Override
    public int getMainLogicServerId() {
        return loadBalanceDAO.getMainLogicServerId();
    }


}
