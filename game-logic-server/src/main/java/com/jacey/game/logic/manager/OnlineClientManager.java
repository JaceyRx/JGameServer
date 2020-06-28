package com.jacey.game.logic.manager;

import akka.actor.ActorRef;
import com.jacey.game.common.manager.IManager;
import com.jacey.game.db.service.LogicServerLoadBalanceService;
import com.jacey.game.db.service.impl.LogicServerLoadBalanceServiceImpl;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: 在线客户端管理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class OnlineClientManager implements IManager {

    private OnlineClientManager(){}

    private static OnlineClientManager instance = new OnlineClientManager();

    public static OnlineClientManager getInstance() {
        return instance;
    }

    private LogicServerLoadBalanceService logicServerLoadBalanceService;

    // key:sessionId,
    // value:这个客户端对应gateway中ResponseActor（玩家发的消息，gateway转发到logicServer，sender为ResponseActor）
    private final Map<Integer, ActorRef> sessionIdToGatewayResponseActor = new ConcurrentHashMap<Integer, ActorRef>();


    @Override
    public void init() {
        logicServerLoadBalanceService = SpringManager.getInstance().getBean(LogicServerLoadBalanceServiceImpl.class);
    }

    @Override
    public void shutdown() {
        // 服务器关闭时，清除服务器连接地址、负载信息
        logicServerLoadBalanceService.removeOneLogicServerLoadBalance(ConfigManager.LOGIC_SERVER_ID);
        logicServerLoadBalanceService.removeOneLogicServerIdToAkkaPath(ConfigManager.LOGIC_SERVER_ID);
        if (ConfigManager.IS_MAIN_LOGIC_SERVER == true
                && logicServerLoadBalanceService.getMainLogicServerId() == ConfigManager.LOGIC_SERVER_ID) {
            logicServerLoadBalanceService.setMainLogicServerId(0);
        }
        // 清除连接本logicServer的玩家的sessionId与logicServerId的对应信息
        for (int sessionId : sessionIdToGatewayResponseActor.keySet()) {
            logicServerLoadBalanceService.removeOneSessionIdToLogicServerId(sessionId);
        }
    }

    /**
     * 获取在线用户数
     * @return
     */
    public int getOnlineActorCount() {
        return sessionIdToGatewayResponseActor.size();
    }

    /**
     * sessid 与 responseActor绑定
     * @param sessionId
     * @param gatewayResponseActor
     */
    public void addSessionIdToGatewayResponseActor(int sessionId, ActorRef gatewayResponseActor) {
        sessionIdToGatewayResponseActor.put(sessionId, gatewayResponseActor);
        // SessionId与logicServerId绑定
        logicServerLoadBalanceService.setOneSessionIdToLogicServerId(sessionId, ConfigManager.LOGIC_SERVER_ID);
        if (MessageManager.getInstance().isAvailableForUpdateLoadBalance() == true) {
            logicServerLoadBalanceService.setOneLogicServerLoadBalance(ConfigManager.LOGIC_SERVER_ID,
                    sessionIdToGatewayResponseActor.size());
        }
    }

    public void removeSessionIdToGatewayResponseActor(int sessionId) {
        sessionIdToGatewayResponseActor.remove(sessionId);
        logicServerLoadBalanceService.removeOneSessionIdToLogicServerId(sessionId);
        if (MessageManager.getInstance().isAvailableForUpdateLoadBalance() == true) {
            logicServerLoadBalanceService.setOneLogicServerLoadBalance(ConfigManager.LOGIC_SERVER_ID,
                    sessionIdToGatewayResponseActor.size());
        }
    }


    public ActorRef getGatewayResponseActor(int sessionId) {
        return sessionIdToGatewayResponseActor.get(sessionId);
    }

}
