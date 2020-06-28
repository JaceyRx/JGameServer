package com.jacey.game.gm.controller;

import com.jacey.game.db.service.GatewayServerLoadBalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description: 供客户端调用的接口
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@RestController
public class ClientController {

    @Autowired
    private GatewayServerLoadBalanceService gatewayServerLoadBalanceService;

    /**
     * 获取空闲网关服务器连接地址
     * @return
     */
    @RequestMapping("/gateway")
    public String getLeisureGateway() {
        Integer leisureGatewayId = gatewayServerLoadBalanceService.getLeisureGatewayId();
        return leisureGatewayId == null ? null : gatewayServerLoadBalanceService.getOneGatewayIdToConnectPath(leisureGatewayId);
    }

}
