package com.jacey.game.db.service;

import java.util.Map;

/**
 * @Description: 网关服务器服务在操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface GatewayServerLoadBalanceService {

    /**
     * 设置sessionId与gatewayId绑定
     * @param sessionId
     * @param gatewayId
     */
    public void setOneSessionIdToGatewayId(int sessionId, int gatewayId);

    /**
     * 获取该SessionId所绑定的Gateway网关id
     * @param sessionId
     * @return
     */
    public Integer getOneSessionIdToGatewayId(int sessionId);

    /**
     * 移除该session与Gateway网关Id的绑定关系
     * @param sessionId
     */
    public void removeOneSessionIdToGatewayId(int sessionId);

    /**
     * 设置某个Gateway网关服务器负载情况
     * @param gatewayId  网关服务器id
     * @param count		 负载情况（连接数）
     */
    public void setOneGatewayLoadBalance(int gatewayId, int count);

    /**
     * 修改某个Gateway网关服务器的负载情况
     * @param gatewayId		网关服务器id
     * @param changeCount  	负载情况修改数值（连接数修改）
     */
    public void changeOneGatewayLoadBalance(int gatewayId, int changeCount);

    /**
     * 移除某个Gateway网关服务器负载情况
     * @param gatewayId 网关服务器id
     */
    public void removeOneGatewayLoadBalance(int gatewayId);

    /**
     * 获取所有Gateway网关服务器的负载信息
     * @return
     */
    public Map<Integer, Integer> getAllGatewayLoadBalance();

    /**
     * 清除Gateway网关服务器的所有负载信息
     */
    public void cleanGatewayLoadBalance();

    /**
     * 获取Gateway空闲网关服务器id
     * @return
     */
    public Integer getLeisureGatewayId();

    void setOneGatewayIdToAkkaPath(int gatewayId, String akkaPath);

    String getOneGatewayIdToAkkaPath(int gatewayId);

    Map<Integer, String> getAllGatewayIdToAkkaPath();

    void removeOneGatewayIdToAkkaPath(int gatewayId);

    void cleanGatewayIdToAkkaPath();

    /**
     * 设置网关服务器id与其连接地址绑定
     * @param gatewayId		网关服务器id
     * @param connectPath	连接地址（ip:port）
     */
    public void setOneGatewayIdToConnectPath(int gatewayId, String connectPath);

    /**
     * 根据网关服务器id，获取其连接地址
     * @param gatewayId  	网关服务器id
     * @return
     */
    public String getOneGatewayIdToConnectPath(int gatewayId);

    /**
     * 根据网关服务器id,移除其与连接地址的绑定关系
     * @param gatewayId		网关服务器id
     */
    public void removeOneGatewayIdToConnectPath(int gatewayId);


    /**
     * 清除所有网关服务器id与连接地址的绑定关系
     */
    public void cleanGatewayIdToConnectPath();
}
