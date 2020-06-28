package com.jacey.game.db.service.impl;

import com.jacey.game.db.dao.GatewayServerLoadBalanceDAO;
import com.jacey.game.db.service.GatewayServerLoadBalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Description: 网关服务器服务在操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Service
public class GatewayServerLoadBalanceServiceImpl implements GatewayServerLoadBalanceService {

    @Autowired
    private GatewayServerLoadBalanceDAO loadBalanceDAO;

    /**
     * 设置sessionId与gatewayId绑定
     * @param sessionId
     * @param gatewayId
     */
    @Override
    public void setOneSessionIdToGatewayId(int sessionId, int gatewayId) {
        loadBalanceDAO.setOneSessionIdToGatewayId(sessionId, gatewayId);
    }

    /**
     * 获取该SessionId所绑定的Gateway网关id
     * @param sessionId
     * @return
     */
    @Override
    public Integer getOneSessionIdToGatewayId(int sessionId) {
        return loadBalanceDAO.getOneSessionIdToGatewayId(sessionId);
    }

    /**
     * 移除该session与Gateway网关Id的绑定关系
     * @param sessionId
     */
    @Override
    public void removeOneSessionIdToGatewayId(int sessionId) {
        loadBalanceDAO.removeOneSessionIdToGatewayId(sessionId);
    }


    /**
     * 设置某个Gateway网关服务器负载情况
     * @param gatewayId  网关服务器id
     * @param count		 负载情况（连接数）
     */
    @Override
    public void setOneGatewayLoadBalance(int gatewayId, int count) {
        loadBalanceDAO.setOneGatewayLoadBalance(gatewayId, count);
    }

    /**
     * 修改某个Gateway网关服务器的负载情况
     * @param gatewayId		网关服务器id
     * @param changeCount  	负载情况修改数值（连接数修改）
     */
    @Override
    public void changeOneGatewayLoadBalance(int gatewayId, int changeCount) {
        loadBalanceDAO.changeOneGatewayLoadBalance(gatewayId, changeCount);
    }


    /**
     * 移除某个Gateway网关服务器负载情况
     * @param gatewayId 网关服务器id
     */
    @Override
    public void removeOneGatewayLoadBalance(int gatewayId) {
        loadBalanceDAO.removeOneGatewayLoadBalance(gatewayId);
    }


    /**
     * 获取所有Gateway网关服务器的负载信息
     * @return
     */
    @Override
    public Map<Integer, Integer> getAllGatewayLoadBalance() {
        return loadBalanceDAO.getAllGatewayLoadBalance();
    }


    /**
     * 清除Gateway网关服务器的所有负载信息
     */
    @Override
    public void cleanGatewayLoadBalance() {
        loadBalanceDAO.cleanGatewayLoadBalance();
    }

    /**
     * 获取Gateway空闲网关服务器id
     * @return
     */
    @Override
    public Integer getLeisureGatewayId() {
        return loadBalanceDAO.getLeisureGatewayId();
    }

    @Override
    public void setOneGatewayIdToAkkaPath(int gatewayId, String akkaPath) {
        loadBalanceDAO.setOneGatewayIdToAkkaPath(gatewayId, akkaPath);
    }

    @Override
    public String getOneGatewayIdToAkkaPath(int gatewayId) {
        return loadBalanceDAO.getOneGatewayIdToAkkaPath(gatewayId);
    }

    @Override
    public Map<Integer, String> getAllGatewayIdToAkkaPath() {
        return loadBalanceDAO.getAllGatewayIdToAkkaPath();
    }

    @Override
    public void removeOneGatewayIdToAkkaPath(int gatewayId) {
        loadBalanceDAO.removeOneGatewayIdToAkkaPath(gatewayId);
    }

    @Override
    public void cleanGatewayIdToAkkaPath() {
        loadBalanceDAO.cleanGatewayIdToAkkaPath();
    }

    /**
     * 设置网关服务器id与其连接地址绑定
     * @param gatewayId		网关服务器id
     * @param connectPath	连接地址（ip:port）
     */
    @Override
    public void setOneGatewayIdToConnectPath(int gatewayId, String connectPath) {
        loadBalanceDAO.setOneGatewayIdToConnectPath(gatewayId, connectPath);
    }

    /**
     * 根据网关服务器id，获取其连接地址
     * @param gatewayId  	网关服务器id
     * @return
     */
    @Override
    public String getOneGatewayIdToConnectPath(int gatewayId) {
        return loadBalanceDAO.getOneGatewayIdToConnectPath(gatewayId);
    }

    /**
     * 根据网关服务器id,移除其与连接地址的绑定关系
     * @param gatewayId		网关服务器id
     */
    @Override
    public void removeOneGatewayIdToConnectPath(int gatewayId) {
        loadBalanceDAO.removeOneGatewayIdToConnectPath(gatewayId);
    }

    /**
     * 清除所有网关服务器id与连接地址的绑定关系
     */
    @Override
    public void cleanGatewayIdToConnectPath() {
        loadBalanceDAO.cleanGatewayIdToConnectPath();
    }
}
