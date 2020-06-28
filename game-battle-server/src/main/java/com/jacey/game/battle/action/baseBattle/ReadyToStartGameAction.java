package com.jacey.game.battle.action.baseBattle;

import com.jacey.game.battle.service.BattleEventService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @Description: 确认可以开始游戏
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
@Component
@MessageClassMapping(value = Rpc.RpcNameEnum.ReadyToStartGame_VALUE, isNet = false)
public class ReadyToStartGameAction extends BaseMessageAction {

    @Autowired
    private BattleServerLoadBalanceService battleServerLoadBalanceService;
    @Autowired
    private BattleInfoService battleInfoService;
    @Autowired
    private BattleEventService battleEventService;

    @Override
    protected void LogRequest(IMessage requestMessage) throws Exception {
        NetMessage req = (NetMessage) requestMessage;
        log.info("【准备完毕请求】 userId = {}:\n{}", req.getUserId(), req.getProtobufText(CommonMsg.MatchRequest.class));
    }

    @Override
    protected void LogResponse(IMessage responseMessage) throws Exception {
        NetMessage resp = (NetMessage) responseMessage;
        log.info("【准备完毕响应】 userId = {}:\n{}", resp.getUserId(), resp.getProtobufText(CommonMsg.MatchResponse.class));
    }

    @Override
    protected IMessage doAction(IMessage requestMessage) throws Exception {
        NetMessage msg = (NetMessage) requestMessage;
        int userId = msg.getUserId();

        String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
        if (battleId == null) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.UserNotInBattle_VALUE);
        }
        // 获取未准备用户列表
        Set<Integer> notReadyUserIds = battleInfoService.getOneBattleNotReadyUserIds(battleId);
        if (notReadyUserIds == null || notReadyUserIds.contains(userId) == false) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.ReadyToStartGameErrorAlreadyReady_VALUE); // 确认可以开始游戏错误，已经确认过了
        }
        // 未准备玩家列表，移除当前用户
        notReadyUserIds.remove(userId);
        battleInfoService.removeOneBattleNotReadyUserId(battleId, userId);
        // 移除后，再获取一次列表。防止并发问题
        notReadyUserIds = battleInfoService.getOneBattleNotReadyUserIds(battleId);
        if (notReadyUserIds == null || notReadyUserIds.size() < 1) {
            // 开始第一个回合
            battleEventService.startFirstTurn(battleId);
        }

        BaseBattle.ReadyToStartGameResponse.Builder builder = BaseBattle.ReadyToStartGameResponse.newBuilder();
        return super.buildResponseNetMsg(userId, Rpc.RpcNameEnum.ReadyToStartGame_VALUE, builder); //确认可以开始游戏
    }
}
