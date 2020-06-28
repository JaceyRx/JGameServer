package com.jacey.game.db.service;

import com.jacey.game.common.proto3.BaseBattle;
import com.jacey.game.common.proto3.CommonEnum;

import java.util.List;
import java.util.Set;

/**
 * @Description: 对战信息的处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface BattleInfoService {

    /** ====================正在对战Redis列表操作========================== */

    /**
     * 添加BattleId到 正在对战Redis列表中
     * @param battleId
     * @param battleType
     */
    void addPlayingBattleId(String battleId, CommonEnum.BattleTypeEnum battleType);

    /**
     * 从正在对战Redis列表中，移除当前BattleId
     * @param battleId
     * @param battleType
     */
    void removePlayingBattleId(String battleId, CommonEnum.BattleTypeEnum battleType);

    /** ====================某一个对战所属玩家UserId List 操作========================== */

    /**
     * 设置当前对战玩家的UserId List
     * @param battleId
     * @param userIds
     */
    void initOneBattleUserIds(String battleId, List<Integer> userIds);

    /**
     * 获取当前对战玩家的UserId List
     * @param battleId
     * @return
     */
    List<Integer> getOneBattleUserIds(String battleId);

    /**
     * 清除当前对战的UserId List
     * @param battleId
     */
    void cleanOneBattleUserIds(String battleId);

    /** ====================当前回合信息操作========================== */

    /**
     * 设置当前回合信息
     * @param battleId
     * @param info
     */
    void setBattleCurrentTurnInfo(String battleId, BaseBattle.CurrentTurnInfo info);

    /**
     * 回去当前回合信息
     * @param battleId
     * @return
     */
    BaseBattle.CurrentTurnInfo getBattleCurrentTurnInfo(String battleId) throws Exception;

    /**
     * 移除当前回合信息
     * @param battleId
     */
    void removeBattleCurrentTurnInfo(String battleId);

    /** ====================棋盘信息操作========================== */

    /**
     * 设置一个棋盘信息
     * @param battleId
     * @param index
     * @param value
     */
    void setOneBattleCellInfo(String battleId, int index, int value);

    /**
     * 设置整个棋盘信息（初始化棋盘）
     * @param battleId
     * @param allCellInfo
     */
    void initAllBattleCellInfo(String battleId, List<Integer> allCellInfo);

    /**
     * 获取单个棋盘信息
     * @param battleId
     * @param index
     * @return
     */
    Integer getOneBattleCellInfo(String battleId, int index);

    /**
     * 获取整个棋盘信息
     * @param battleId
     * @return
     */
    List<Integer> getAllBattleCellInfo(String battleId);

    /**
     * 清除整个棋盘信息
     * @param battleId
     */
    void cleanAllBattleCellInfo(String battleId);

    /** ====================对战事件操作========================== */

    /**
     * 添加一个对战事件
     * @param battleId
     * @param eventMsg
     */
    void addOneBattleEvent(String battleId, BaseBattle.EventMsg eventMsg);

    /**
     * 获取下一个事件编号（增加1并返回）
     * @param battleId
     * @return
     */
    int addAndGetNextAvailableEventNum(String battleId);

    /**
     * 获取最后一个事件编号
     * @param battleId
     * @return
     */
    int getLastEventNum(String battleId);

    /**
     * 移除最后一个事件编号
     * @param battleId
     */
    void removeLastEventNum(String battleId);

    /** ====================对战开始时间操作========================== */

    void setOneBattleStartTimestamp(String battleId, long startTimestamp);

    long getOneBattleStartTimestamp(String battleId);

    void removeOneBattleStartTimestamp(String battleId);

    /** ====================对战未准备玩家操作========================== */

    /**
     * 设置对战未准备玩家
     * @param battleId
     * @param userIds
     */
    void initOneBattleNotReadyUserIds(String battleId, List<Integer> userIds);

    /**
     * 获取对战未准备玩家
     * @param battleId
     * @return
     */
    Set<Integer> getOneBattleNotReadyUserIds(String battleId);

    /**
     * 移除对战未准备单个玩家
     * @param battleId
     * @return
     */
    void removeOneBattleNotReadyUserId(String battleId, int userId);

    /**
     * 移除对战所有未准备玩家
     * @param battleId
     * @return
     */
    void cleanOneBattleNotReadyUserIds(String battleId);

    /** ====================获取对手Id操作========================== */

    /**
     * 获取当前对战所有对手id
     * @param battleId
     * @param userId
     * @return
     */
    List<Integer> getOneUserAllOpponentUserIds(String battleId, Integer userId);

    /**
     * 获取单个对手id
     * @param battleId
     * @param userId
     * @return
     */
    int getOneUserOneOpponentUserId(String battleId, Integer userId);

    /** ====================行动顺序相关========================== */
    /**
     * 获取某个玩家在某场战斗的行动顺序（先手为1，依次递增）
     * @param battleId
     * @param userId
     * @return
     */
    int getOneUserSeq(String battleId, Integer userId);

    /**
     * 根据某场对战中的顺序获取userId
     * @param battleId
     * @param seq
     * @return
     */
    int getOneUserIdBySeq(String battleId, int seq);



}
