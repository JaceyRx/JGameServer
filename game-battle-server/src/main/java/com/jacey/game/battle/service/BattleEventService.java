package com.jacey.game.battle.service;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageV3;
import com.jacey.game.common.exception.RpcErrorException;
import com.jacey.game.common.proto3.BaseBattle;
import com.jacey.game.common.proto3.RemoteServer;

/**
 * @Description: 用于对战事件处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface BattleEventService {


    /**
     * 初始化对战
     * @param battleRoomInfo
     */
    void initBattle(RemoteServer.BattleRoomInfo battleRoomInfo);

    /**
     * 开始第一回合（0回合，特殊处理。由最后准备完成。玩家调用）
     * @param battleId
     */
    void startFirstTurn(String battleId) throws Exception;

    /**
     * 执行事件
     * @param battleId
     * @param firstEventBuilder
     * @return
     */
    BaseBattle.EventMsgList.Builder doEvent(String battleId, BaseBattle.EventMsg.Builder firstEventBuilder) throws Exception;

    /**
     * 构造事件
     * @param battleId
     * @param eventType
     * @param eventBuilder
     * @return
     */
    BaseBattle.EventMsg.Builder buildOneEvent(String battleId, BaseBattle.EventTypeEnum eventType,
                                              GeneratedMessageV3.Builder eventBuilder) throws RpcErrorException;

    /**
     * 推事事件消息列表给某一对战的所有玩家
     * @param battleId
     * @param eventMsgListBuilder
     */
    void pushEventMsgListToAllBattlePlayers(String battleId, BaseBattle.EventMsgList.Builder eventMsgListBuilder);

    /**
     * 推送事件消息列表给某一对战的单个玩家
     * @param userId
     * @param eventMsgListBuilder
     */
    void pushEventMsgListToOneBattlePlayer(int userId, BaseBattle.EventMsgList.Builder eventMsgListBuilder);

    /**
     * 推送事件消息列表给某一对战的所有对手
     * @param battleId
     * @param userId
     * @param eventMsgListBuilder
     */
    void pushEventMsgListToAllOpponentBattlePlayers(String battleId, int userId,
                                                    BaseBattle.EventMsgList.Builder eventMsgListBuilder);

}
