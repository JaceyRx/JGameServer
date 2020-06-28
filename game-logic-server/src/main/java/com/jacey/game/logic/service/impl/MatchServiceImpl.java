package com.jacey.game.logic.service.impl;

import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.service.PlayStateService;
import com.jacey.game.logic.manager.MessageManager;
import com.jacey.game.logic.service.MatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @Description: 匹配处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
@Service
public class MatchServiceImpl implements MatchService {

    @Autowired
    private PlayStateService playStateService;
    /** 暂停匹配标记 */
    private boolean isStopMatch = false;
    /** 匹配任务队列 */
    private final Queue<Integer> matchTwoPlayerBattleUserIdsQueue = new LinkedBlockingDeque<>();

    @Override
    public void doMatch() throws Exception {
        // 是否停止匹配
        if (isStopMatch == false) {
            // 简单1v1战斗匹配
            doSimpleTwoPlayerBattleMatch();
        }
    }

    @Override
    public boolean addMatchPlayer(int userId, CommonEnum.BattleTypeEnum battleType) throws Exception {
        switch (battleType.getNumber()) {
            case CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer_VALUE: {   //1v1
                matchTwoPlayerBattleUserIdsQueue.add(userId);
                break;
            } default: {
                log.error("【匹配任务添加失败】 not support battleType = {}", battleType);
                return false;
            }
        }
        // 修改玩家action状态为匹配中
        playStateService.changeUserActionState(userId,
                CommonEnum.UserActionStateEnum.Matching_VALUE,
                CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer_VALUE, null);
        return true;
    }

    @Override
    public boolean removeMatchPlayer(int userId, CommonEnum.BattleTypeEnum battleType) throws Exception {
        boolean isRemoveSuccess = false;
        switch (battleType.getNumber()) {
            case CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer_VALUE: {
                isRemoveSuccess = matchTwoPlayerBattleUserIdsQueue.remove(userId);
                break;
            }
            default: {
                log.error("【匹配任务移除失败】 not support battleType = {}", battleType);
                return false;
            }
        }
        if (isRemoveSuccess == true) {
            // 清除玩家匹配中的action状态
            playStateService.changeUserActionState(userId,
                    CommonEnum.UserActionStateEnum.ActionNone_VALUE,
                    CommonEnum.BattleTypeEnum.NoneType_VALUE, null);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void stopMatch() throws Exception {
        isStopMatch = true;
        for (int userId : matchTwoPlayerBattleUserIdsQueue) {
            playStateService.changeUserActionState(userId,
                    CommonEnum.UserActionStateEnum.ActionNone_VALUE,
                    CommonEnum.BattleTypeEnum.NoneType_VALUE, null);
        }
    }

    /**
     * 简单匹配逻辑
     * @throws Exception
     */
    private void doSimpleTwoPlayerBattleMatch() throws Exception {
        // 但匹配玩家大于1时才执行
        if (matchTwoPlayerBattleUserIdsQueue.size() > 1) {
            // 获取两名玩家
            List<Integer> matchUserIds = new ArrayList<Integer>();
            matchUserIds.add(matchTwoPlayerBattleUserIdsQueue.poll());
            matchUserIds.add(matchTwoPlayerBattleUserIdsQueue.poll());
            // 随机调整先后顺序
            Collections.shuffle(matchUserIds);
            log.info("【1v1对战匹配】 userId = {} and {}", matchUserIds.get(0), matchUserIds.get(1));
            // 发起匹配
            doAfterMatchSuccess(CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer, matchUserIds);
        }
    }

    /**
     * 匹配后续动作
     * @param battleType
     * @param userIds
     * @throws Exception
     */
    private void doAfterMatchSuccess(CommonEnum.BattleTypeEnum battleType, List<Integer> userIds) throws Exception {
        // 构造BattleId
        String battleId = generateBattleId(battleType);
        // 通知BattleServer初始化战场
        Boolean isSuccess =  MessageManager.getInstance().noticeBattleServerCreateNewBattle(battleType, battleId, userIds);
        if (isSuccess) {
            // 将玩家标记为对战状态
            for (int userId : userIds) {
                playStateService.changeUserActionState(userId,
                        CommonEnum.UserActionStateEnum.Playing_VALUE,
                        CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer_VALUE, battleId);
            }
        } else {
            sendMatchFailPush(battleType, userIds); 	// 发送匹配失败信息
        }
    }

    /**
     * 匹配失败消息推送
     * @param battleType
     * @param userIds
     */
    private void sendMatchFailPush(CommonEnum.BattleTypeEnum battleType, List<Integer> userIds) {
        CommonMsg.MatchResultPush.Builder pushBuilder = CommonMsg.MatchResultPush.newBuilder();
        pushBuilder.setIsSuccess(false);
        pushBuilder.setBattleType(battleType);
        NetMessage netMsg = new NetMessage(Rpc.RpcNameEnum.MatchResultPush_VALUE, pushBuilder);

        for (int userId : userIds) {
            MessageManager.getInstance().sendNetMsgToOneUser(userId, netMsg, CommonMsg.MatchResultPush.class);
        }
    }

    /**
     * BattleId 生成器
     * @param battleType
     * @return
     */
    private String generateBattleId(CommonEnum.BattleTypeEnum battleType) {
        return battleType.getNumber() + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
