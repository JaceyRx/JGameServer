package com.jacey.game.db.service.impl;

import com.jacey.game.common.proto3.BaseBattle;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.db.dao.BattleInfoDAO;
import com.jacey.game.db.service.BattleInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @Description: 对战信息 Redis操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Service
@Slf4j
public class BattleInfoServiceImpl implements BattleInfoService {

    @Autowired
    private BattleInfoDAO battleInfoDAO;


    @Override
    public void addPlayingBattleId(String battleId, CommonEnum.BattleTypeEnum battleType) {
        battleInfoDAO.addPlayingBattleId(battleId, battleType);
    }

    @Override
    public void removePlayingBattleId(String battleId, CommonEnum.BattleTypeEnum battleType) {
        battleInfoDAO.removePlayingBattleId(battleId, battleType);
    }

    @Override
    public void initOneBattleUserIds(String battleId, List<Integer> userIds) {
        battleInfoDAO.initOneBattleUserIds(battleId, userIds);
    }

    @Override
    public List<Integer> getOneBattleUserIds(String battleId) {
        return battleInfoDAO.getOneBattleUserIds(battleId);
    }

    @Override
    public void cleanOneBattleUserIds(String battleId) {
        battleInfoDAO.cleanOneBattleUserIds(battleId);
    }

    @Override
    public void setBattleCurrentTurnInfo(String battleId, BaseBattle.CurrentTurnInfo info) {
        battleInfoDAO.setBattleCurrentTurnInfo(battleId, info);
    }

    @Override
    public BaseBattle.CurrentTurnInfo getBattleCurrentTurnInfo(String battleId) throws Exception {
        return battleInfoDAO.getBattleCurrentTurnInfo(battleId);
    }

    @Override
    public void removeBattleCurrentTurnInfo(String battleId) {
        battleInfoDAO.removeBattleCurrentTurnInfo(battleId);
    }

    @Override
    public void setOneBattleCellInfo(String battleId, int index, int value) {
        battleInfoDAO.setOneBattleCellInfo(battleId, index, value);
    }

    @Override
    public void initAllBattleCellInfo(String battleId, List<Integer> allCellInfo) {
        battleInfoDAO.initAllBattleCellInfo(battleId, allCellInfo);
    }

    @Override
    public Integer getOneBattleCellInfo(String battleId, int index) {
        return battleInfoDAO.getOneBattleCellInfo(battleId, index);
    }

    @Override
    public List<Integer> getAllBattleCellInfo(String battleId) {
        return battleInfoDAO.getAllBattleCellInfo(battleId);
    }

    @Override
    public void cleanAllBattleCellInfo(String battleId) {
        battleInfoDAO.cleanAllBattleCellInfo(battleId);
    }

    @Override
    public void addOneBattleEvent(String battleId, BaseBattle.EventMsg eventMsg) {
        battleInfoDAO.addOneBattleEvent(battleId, eventMsg);
    }

    @Override
    public int addAndGetNextAvailableEventNum(String battleId) {
        return battleInfoDAO.addAndGetNextAvailableEventNum(battleId);
    }

    @Override
    public int getLastEventNum(String battleId) {
        return battleInfoDAO.getLastEventNum(battleId);
    }

    @Override
    public void removeLastEventNum(String battleId) {
        battleInfoDAO.removeLastEventNum(battleId);
    }

    @Override
    public void setOneBattleStartTimestamp(String battleId, long startTimestamp) {
        battleInfoDAO.setOneBattleStartTimestamp(battleId, startTimestamp);
    }

    @Override
    public long getOneBattleStartTimestamp(String battleId) {
        return battleInfoDAO.getOneBattleStartTimestamp(battleId);
    }

    @Override
    public void removeOneBattleStartTimestamp(String battleId) {
        battleInfoDAO.removeOneBattleStartTimestamp(battleId);
    }

    @Override
    public void initOneBattleNotReadyUserIds(String battleId, List<Integer> userIds) {
        Integer[] array = userIds.toArray(new Integer[userIds.size()]);
        battleInfoDAO.initOnebattleNotReadyUserIds(battleId, array);
    }

    @Override
    public Set<Integer> getOneBattleNotReadyUserIds(String battleId) {
        return battleInfoDAO.getOnebattleNotReadyUserIds(battleId);
    }

    @Override
    public void removeOneBattleNotReadyUserId(String battleId, int userId) {
        battleInfoDAO.removeOnebattleNotReadyUserId(battleId, userId);
    }

    @Override
    public void cleanOneBattleNotReadyUserIds(String battleId) {
        battleInfoDAO.cleanOnebattleNotReadyUserIds(battleId);
    }

    @Override
    public List<Integer> getOneUserAllOpponentUserIds(String battleId, Integer userId) {
        List<Integer> userIds = new ArrayList<Integer>(battleInfoDAO.getOneBattleUserIds(battleId));
        userIds.remove(userId);
        return userIds;
    }

    @Override
    public int getOneUserOneOpponentUserId(String battleId, Integer userId) {
        return getOneUserAllOpponentUserIds(battleId, userId).get(0);
    }

    @Override
    public int getOneUserSeq(String battleId, Integer userId) {
        List<Integer> userIds = battleInfoDAO.getOneBattleUserIds(battleId);
        return userIds.indexOf(userId) + 1;
    }

    @Override
    public int getOneUserIdBySeq(String battleId, int seq) {
        List<Integer> userIds = battleInfoDAO.getOneBattleUserIds(battleId);
        return userIds.get(seq - 1);
    }
}
