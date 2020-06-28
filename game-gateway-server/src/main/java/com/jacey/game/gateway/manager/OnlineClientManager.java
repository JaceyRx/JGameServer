package com.jacey.game.gateway.manager;

import com.jacey.game.common.manager.IManager;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.db.service.*;
import com.jacey.game.db.service.impl.*;
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

    // key:sessionId, value:玩家对应的ChannelActor
    /** 内部session缓存 */
    private final Map<Integer, Channel> sessionIdToChannelMap = new ConcurrentHashMap<Integer, Channel>();

    private SessionIdService sessionIdService;
    private GatewayServerLoadBalanceService gatewayServerLoadBalanceService;
    private LogicServerLoadBalanceService logicServerLoadBalanceService;
    private BattleServerLoadBalanceService battleServerLoadBalanceService;
    private ChatServerLoadBalanceService chatServerLoadBalanceService;
    private PlayStateService playStateService;

    @Override
    public void init() {
        // 加载服务器负载service
        gatewayServerLoadBalanceService = SpringManager.getInstance().getBean(GatewayServerLoadBalanceServiceImpl.class);
        logicServerLoadBalanceService = SpringManager.getInstance().getBean(LogicServerLoadBalanceServiceImpl.class);
        battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceServiceImpl.class);
        chatServerLoadBalanceService = SpringManager.getInstance().getBean(ChatServerLoadBalanceServiceImpl.class);
        sessionIdService = SpringManager.getInstance().getBean(SessionIdServiceImpl.class);
        playStateService = SpringManager.getInstance().getBean(PlayStateServiceImpl.class);
    }

    @Override
    public void shutdown() {

    }

    /**
     * 获取当前在线客户端数
     * @return
     */
    public int getOnlineSessionCount() {
        return sessionIdToChannelMap.size();
    }

    /**
     * 用于SessionId与channel、GatewayId绑定。并更新服务器负载
     * @param sessionId
     * @param channel
     */
    public void addSession(int sessionId, Channel channel) {
        // sessionId与channel绑定
        sessionIdToChannelMap.put(sessionId, channel);
        // 【redis】设置sessionId与GatewayId绑定
        gatewayServerLoadBalanceService.setOneSessionIdToGatewayId(sessionId, ConfigManager.GATEWAY_ID);
        // 更新服务器负债
        if (MessageManager.getInstance().isAvailableForUpdateLoadBalance() == true) {
            gatewayServerLoadBalanceService.setOneGatewayLoadBalance(ConfigManager.GATEWAY_ID, sessionIdToChannelMap.size());
        }
    }

    /**
     * Session 断线处理
     * @param sessionId
     */
    public void removeSession(int sessionId) {
        // 移除Session缓存
        sessionIdToChannelMap.remove(sessionId);
        // 移除sessionId与GatewayServerId 的绑定关系
        gatewayServerLoadBalanceService.removeOneSessionIdToGatewayId(sessionId);
        // 获取当前sessionID绑定的userId
        Integer userId = sessionIdService.getOneSessionIdToUserId(sessionId);
        // 移除SessionId与UserId的绑定关系
        sessionIdService.removeOneSessionIdToUserId(sessionId);
        // 同一账号二次登录强制断线状态更新处理逻辑
        if (userId != null) {
            // 这里必须判断当前userId对应的最新sessionId是不是这个sessionId，因为处理同一账号二次登录后旧连接断开的情况时，不应该删除userId与最新sessionId的对应
            int userIdToSessionId = sessionIdService.getOneUserIdToSessionId(userId);
            // 正常下线（非同一账号二次登录）
            if (userIdToSessionId == sessionId) {
                // 移除userId与sessionId的绑定关系
                sessionIdService.removeOneUserIdToSessionId(userId);
                // 修改玩家状态为离线
                playStateService.changeUserOnlineState(userId, false);
                // 通知LogicServer / battleServer /chatServer 客户端已离线
                noticeClientOffline(sessionId, userId, true);
            } else {
                /** 同一账号二次登录 -- 玩家状态不做更新 */
                noticeClientOffline(sessionId, userId, false);
            }
        } else {
            /** 未登录用户 */
            noticeClientOffline(sessionId, null, false);
        }
        // 负载更新
        if (MessageManager.getInstance().isAvailableForUpdateLoadBalance() == true) {
            gatewayServerLoadBalanceService.setOneGatewayLoadBalance(ConfigManager.GATEWAY_ID, sessionIdToChannelMap.size());
        }
    }

    /**
     * 如果离线的客户端已连上logicServer或 battleServer，需要进行通知
     * @param userId        如果该session对应的客户端登录成功，则需传此字段
     * @param isUserOffline 是否是玩家也要下线（同一账号二次登录导致旧session断开，但对应玩家仍在线）
     */
    private void noticeClientOffline(Integer sessionId, Integer userId, boolean isUserOffline) {
        Integer connectedLogicServerId = logicServerLoadBalanceService.getOneSessionIdToLogicServerId(sessionId);
        // logic服务器通知
        if (connectedLogicServerId != null) {
            // 构造gateway离线客户端推送消息
            RemoteServer.GatewayNoticeClientOfflinePush.Builder pushBuilder = RemoteServer.GatewayNoticeClientOfflinePush.newBuilder();
            pushBuilder.setSessionId(sessionId);
            // 已登录用户
            if (userId != null) {
                pushBuilder.setUserId(userId);
            } else {
                pushBuilder.setUserId(0);  // 由于proto3中没有hasXxx方法判断字段是否有值，所以给UserId字段设置默认值。好做判断
            }
            // 是否让玩家下线
            pushBuilder.setIsUserOffline(isUserOffline);
            RemoteMessage remoteMsg = new RemoteMessage(RemoteServer.RemoteRpcNameEnum.RemoteRpcGatewayNoticeClientOfflinePush_VALUE, pushBuilder);
            if (MessageManager.getInstance().sendRemoteMsgToLogicServer(remoteMsg, connectedLogicServerId) == false) {
                log.error(
                        "【客户端断线Gateway推送失败】 无法推送消息到 logic Server, sessionId = {}, logicServerId = {}",
                        sessionId, connectedLogicServerId);
            }
        }
        // battle服务器通知
        if (userId != null) {
            String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
            if (battleId != null) {
                Integer connectedBattleServerId = battleServerLoadBalanceService.getOneBattleIdToBattleServerId(battleId);
                if (connectedBattleServerId != null) {
                    RemoteServer.GatewayNoticeClientOfflinePush.Builder pushBuilder = RemoteServer.GatewayNoticeClientOfflinePush.newBuilder();
                    pushBuilder.setSessionId(sessionId);
                    pushBuilder.setUserId(userId);
                    pushBuilder.setIsUserOffline(isUserOffline);
                    RemoteMessage remoteMsg = new RemoteMessage(
                            RemoteServer.RemoteRpcNameEnum.RemoteRpcGatewayNoticeClientOfflinePush_VALUE, pushBuilder);
                    if (MessageManager.getInstance().sendRemoteMsgToBattleServer(remoteMsg,
                            connectedLogicServerId) == false) {
                        log.error(
                                "【客户端断线Gateway推送失败】,无法推送消息到 battle Server, sessionId = {}, userId = {}, battleId = {}, battleServerId = {}",
                                sessionId, userId, battleId, connectedBattleServerId);
                    }
                }
            }
        }

        if (userId != null) {
            String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
            if (battleId != null) {
                Integer connectedChatServerId = chatServerLoadBalanceService.getOneBattleIdToChatServerId(battleId);
                if (connectedChatServerId != null) {
                    RemoteServer.GatewayNoticeClientOfflinePush.Builder pushBuilder = RemoteServer.GatewayNoticeClientOfflinePush.newBuilder();
                    pushBuilder.setSessionId(sessionId);
                    pushBuilder.setUserId(userId);
                    pushBuilder.setIsUserOffline(isUserOffline);
                    RemoteMessage remoteMsg = new RemoteMessage(
                            RemoteServer.RemoteRpcNameEnum.RemoteRpcGatewayNoticeClientOfflinePush_VALUE, pushBuilder);
                    if (MessageManager.getInstance().sendRemoteMsgToChatServer(remoteMsg,
                            connectedLogicServerId) == false) {
                        log.error(
                                "【客户端断线Gateway推送失败】,无法推送消息到 battle Server, sessionId = {}, userId = {}, battleId = {}, battleServerId = {}",
                                sessionId, userId, battleId, connectedChatServerId);
                    }
                }
            }
        }
    }

    public Channel getChannel(int sessionId) {
        return sessionIdToChannelMap.get(sessionId);
    }
}
