package com.jacey.game.battle.manager;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.jacey.game.common.manager.IManager;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.impl.BattleServerLoadBalanceServiceImpl;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
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

    // key:battleId, value:这个对战房间对应的BaseBattleActor
    private final Map<String, ActorRef> battleIdToBattleActor = new ConcurrentHashMap<String, ActorRef>();
    // key:sessionId,
    // value:这个客户端对应gateway中ResponseActor（玩家发的消息，gateway转发到battleServer，sender为ResponseActor）
    private final Map<Integer, ActorRef> sessionIdToGatewayResponseActor = new ConcurrentHashMap<Integer, ActorRef>();

    private BattleServerLoadBalanceService battleServerLoadBalanceService;

    @Override
    public void init() {
        battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceServiceImpl.class);
    }

    @Override
    public void shutdown() {
        // 服务器关闭时，清除服务器连接地址、负载信息
        battleServerLoadBalanceService.removeOneBattleServerLoadBalance(ConfigManager.BATTLE_SERVER_ID);
        battleServerLoadBalanceService.removeOneBattleServerIdToAkkaPath(ConfigManager.BATTLE_SERVER_ID);
    }

    /**
     * 添加BattleActor（对战房间添加）
     * @param battleId
     * @param battleActor
     * @param userIds
     */
    public void addBattleActor(String battleId, ActorRef battleActor, List<Integer> userIds) {
        // 1.battleId与BaseBattleActor 绑定
        battleIdToBattleActor.put(battleId, battleActor);
        // 2.当前对战玩家userId 与 battleId绑定
        for (int userId : userIds) {
            // key userId  value battleId
            battleServerLoadBalanceService.setBattleUserIdToBattleId(userId, battleId);
        }
        // 3.battleId与当前Battle Server Id绑定
        battleServerLoadBalanceService.setOneBattleIdToBattleServerId(battleId, ConfigManager.BATTLE_SERVER_ID);
        // 4.服务器负载更新
        if (MessageManager.getInstance().isConnectToGm) {
            battleServerLoadBalanceService.setOneBattleServerLoadBalance(ConfigManager.BATTLE_SERVER_ID, getBattleCount());
        }
    }

    /**
     * 移除对战房间
     * @param battleId
     * @param userIds
     */
    public void removeBattleActor(String battleId, List<Integer> userIds) {
        // 1.battleId与BaseBattleActor 解绑
        ActorRef baseBattleActor = battleIdToBattleActor.get(battleId);
        // 2.毒死当前对战绑定的BaseBattleActor
        baseBattleActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        // 3.当前对战玩家userId 与 battleId解绑
        for (int userId : userIds) {
            // key userId  value battleId
            battleServerLoadBalanceService.removeBattleUserIdToBattleId(userId);
        }
        // 4.battleId与当前Battle Server Id解绑
        battleServerLoadBalanceService.removeOneBattleIdToBattleServerId(battleId);
        // 5.服务器负载更新
        if (MessageManager.getInstance().isConnectToGm) {
            battleServerLoadBalanceService.setOneBattleServerLoadBalance(ConfigManager.BATTLE_SERVER_ID, getBattleCount());
        }
    }

    /**
     * 获取对战房间
     * @param battleId
     * @return
     */
    public ActorRef getBattleActor(String battleId) {
        return battleIdToBattleActor.get(battleId);
    }

    /**
     * 获取房间总数
     * @return
     */
    public int getBattleCount() {
        return battleIdToBattleActor.size();
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
