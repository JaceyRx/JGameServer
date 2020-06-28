package com.jacey.game.chat.manager;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.jacey.game.common.manager.IManager;
import com.jacey.game.db.service.ChatServerLoadBalanceService;
import com.jacey.game.db.service.impl.ChatServerLoadBalanceServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: 在线客户端管理器
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class OnlineClientManager implements IManager {

    private OnlineClientManager(){}

    private static OnlineClientManager instance = new OnlineClientManager();

    public static OnlineClientManager getInstance() {
        return instance;
    }

    // key:battleId, value:这个对战聊天室对应的BattleChatRoomActor
    private final Map<String, ActorRef> battleIdToBattleChatRoomActor = new ConcurrentHashMap<String, ActorRef>();
    // key:sessionId,
    // value:这个客户端对应gateway中ResponseActor（玩家发的消息，gateway转发到ChatServer，sender为ResponseActor）
    private final Map<Integer, ActorRef> sessionIdToGatewayResponseActor = new ConcurrentHashMap<Integer, ActorRef>();

    private ChatServerLoadBalanceService chatServerLoadBalanceService;

    @Override
    public void init() {
        chatServerLoadBalanceService = SpringManager.getInstance().getBean(ChatServerLoadBalanceServiceImpl.class);
    }

    @Override
    public void shutdown() {
        // 服务器关闭时，清除服务器连接地址、负载信息
        chatServerLoadBalanceService.removeOneChatServerLoadBalance(ConfigManager.CHAT_SERVER_ID);
        chatServerLoadBalanceService.removeOneChatServerIdToAkkaPath(ConfigManager.CHAT_SERVER_ID);
    }

    public int getBattleChatRoomCount() {
        return battleIdToBattleChatRoomActor.size();
    }

    public ActorRef getBattleChatRoomActor(String battleId) {
        return battleIdToBattleChatRoomActor.get(battleId);
    }

    /**
     *  添加对战聊天室Actor
     * @param battleId
     * @param battleChatRoomActor
     */
    public void addBattleChatRoomActor(String battleId, ActorRef battleChatRoomActor) {
        // 1.battleId与battleChatRoomActor绑定
        battleIdToBattleChatRoomActor.put(battleId, battleChatRoomActor);
        // 2.当前BattleId与聊天 ChatServerId 绑定
        chatServerLoadBalanceService.setOneBattleIdToChatServerId(battleId, ConfigManager.CHAT_SERVER_ID);
        // 3.负载更新
        if (MessageManager.getInstance().isConnectToGm) {
            chatServerLoadBalanceService.setOneChatServerLoadBalance(ConfigManager.CHAT_SERVER_ID, getBattleChatRoomCount());
        }
    }

    /**
     * 对战聊天室 Actor
     * @param battleId
     */
    public void removeBattleChatRoomActor(String battleId) {
        // 1.battleId与battleChatRoomActor解绑
        ActorRef battleChatRoomActor = battleIdToBattleChatRoomActor.remove(battleId);
        // 2.毒死当前对战绑定的BattleChatRoomActor
        battleChatRoomActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        // 3.当前BattleId与聊天 ChatServerId 解绑‘
        chatServerLoadBalanceService.removeOneBattleIdToChatServerId(battleId);
        // 3.负载更新
        if (MessageManager.getInstance().isConnectToGm) {
            chatServerLoadBalanceService.setOneChatServerLoadBalance(ConfigManager.CHAT_SERVER_ID, getBattleChatRoomCount());
        }
    }

    /**
     * 添加 GatewayResponseActor
     * @param sessionId
     * @param gatewayResponseActor
     */
    public void addSessionIdToGatewayResponseActor(int sessionId, ActorRef gatewayResponseActor) {
        sessionIdToGatewayResponseActor.put(sessionId, gatewayResponseActor);
    }

    /**
     * 移除 GatewayResponseActor
     * @param sessionId
     */
    public void removeSessionIdToGatewayResponseActor(int sessionId) {
        sessionIdToGatewayResponseActor.remove(sessionId);
    }

    /**
     * 获取 GatewayResponseActor
     * @param sessionId
     * @return
     */
    public ActorRef getGatewayResponseActor(int sessionId) {
        return sessionIdToGatewayResponseActor.get(sessionId);
    }


}
