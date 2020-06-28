package com.jacey.game.battle.action.baseBattle;

import com.jacey.game.common.action.BaseMessageAction;
import com.jacey.game.common.annotation.MessageClassMapping;
import com.jacey.game.common.exception.RpcErrorException;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.BaseBattle;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.service.BattleInfoService;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.PlayUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @Description: 获取对战信息
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
@Component
@MessageClassMapping(value = Rpc.RpcNameEnum.GetBattleInfo_VALUE, isNet = false)
public class GetBattleInfoAction extends BaseMessageAction {

    @Autowired
    private BattleServerLoadBalanceService battleServerLoadBalanceService;
    @Autowired
    private PlayUserService playUserService;
    @Autowired
    private BattleInfoService battleInfoService;

    @Override
    protected void LogRequest(IMessage requestMessage) throws Exception {
        NetMessage req = (NetMessage) requestMessage;
        log.info("【获取对战信息请求】 userId = {}:\n{}", req.getUserId(), req.getProtobufText(BaseBattle.GetBattleInfoRequest.class));
    }

    @Override
    protected void LogResponse(IMessage responseMessage) throws Exception {
        NetMessage resp = (NetMessage) responseMessage;
        log.info("【获取对战信息响应】 userId = {}:\n{}", resp.getUserId(), resp.getProtobufText(BaseBattle.GetBattleInfoResponse.class));
    }

    @Override
    protected IMessage doAction(IMessage requestMessage) throws Exception {
        NetMessage msg = (NetMessage) requestMessage;
        int userId = msg.getUserId();
        // 根据userId获取battleId
        String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
        if (battleId == null) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.UserNotInBattle_VALUE);
        }
        BaseBattle.GetBattleInfoResponse.Builder builder = BaseBattle.GetBattleInfoResponse.newBuilder();
        BaseBattle.BattleInfo.Builder battleInfoBuilder = BaseBattle.BattleInfo.newBuilder();
        // 根据对战id，获取对战双方userId
        List<Integer> userIds = battleInfoService.getOneBattleUserIds(battleId);
        for (int oneUserId : userIds) {
            // 根据userId获取用户简介信息
            battleInfoBuilder.addUserBriefInfos(playUserService.getUserBriefInfoByUserId(oneUserId));
        }
        // 设置对战开始时间
        battleInfoBuilder.setBattleStartTimestamp(battleInfoService.getOneBattleStartTimestamp(battleId));
        // 设置当前棋盘中棋子信息
        battleInfoBuilder.addAllBattleCellInfo(battleInfoService.getAllBattleCellInfo(battleId));
        // 设置上一个已发生事件的eventNum
        battleInfoBuilder.setLastEventNum(battleInfoService.getLastEventNum(battleId));
        // 获取未准备用户id List
        Set<Integer> notReadyUserIds = battleInfoService.getOneBattleNotReadyUserIds(battleId);
        // 判断是否有未准备玩家
        if (notReadyUserIds == null || notReadyUserIds.size() < 1) {
            // 设置当前回合信息
            battleInfoBuilder.setCurrentTurnInfo(battleInfoService.getBattleCurrentTurnInfo(battleId));   //设置当前回合的信息
        } else {
            // 设置为准备UserId List
            battleInfoBuilder.addAllNotReadyUserIds(notReadyUserIds);  // 设置为准备玩家ids
        }
        builder.setBattleInfo(battleInfoBuilder);
        return super.buildResponseNetMsg(userId, Rpc.RpcNameEnum.GetBattleInfo_VALUE, builder);
    }
}
