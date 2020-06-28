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

import java.util.List;
import java.util.Set;

/**
 * @Description: 认输
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
@Component
@MessageClassMapping(value = Rpc.RpcNameEnum.Concede_VALUE, isNet = false)
public class ConcedeAction extends BaseMessageAction {

    @Autowired
    private BattleServerLoadBalanceService battleServerLoadBalanceService;
    @Autowired
    private BattleInfoService battleInfoService;
    @Autowired
    private BattleEventService battleEventService;

    @Override
    protected void LogRequest(IMessage requestMessage) throws Exception {
        NetMessage req = (NetMessage) requestMessage;
        log.info("【认输请求】 userId = {}:\n{}", req.getUserId(), req.getProtobufText(BaseBattle.ConcedeRequest.class));
    }

    @Override
    protected void LogResponse(IMessage responseMessage) throws Exception {
        NetMessage resp = (NetMessage) responseMessage;
        log.info("【认输响应】 userId = {}:\n{}", resp.getUserId(), resp.getProtobufText(BaseBattle.ConcedeResponse.class));
    }

    @Override
    protected IMessage doAction(IMessage requestMessage) throws Exception {
        NetMessage msg = (NetMessage) requestMessage;
        int userId = msg.getUserId();

        String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
        if (battleId == null) {
            // 对战不存在
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.UserNotInBattle_VALUE);
        }

        Set<Integer> notReadyUserIds = battleInfoService.getOneBattleNotReadyUserIds(battleId);
        if (notReadyUserIds != null && notReadyUserIds.size() > 0) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.BattleNotStart_VALUE);
        }
        // 这里必须在doGameOverEvent之前取得，否则就会因为redis中对战相关信息被清除而无法取得
        // 获取对手id
        int opponentUserId = battleInfoService.getOneUserOneOpponentUserId(battleId, userId);
        // 构造游戏结束事件
        BaseBattle.GameOverEvent.Builder gameOverEventBuilder = BaseBattle.GameOverEvent.newBuilder();
        gameOverEventBuilder.setGameOverReason(BaseBattle.GameOverReasonEnum.GameOverPlayerConcede);
        gameOverEventBuilder.setWinnerUserId(battleInfoService.getOneUserOneOpponentUserId(battleId, userId));
        BaseBattle.EventMsg.Builder eventMsgBuilder = battleEventService.buildOneEvent(battleId, BaseBattle.EventTypeEnum.EventTypeGameOver,
                gameOverEventBuilder);
        //执行事件
        BaseBattle.EventMsgList.Builder eventMsgListBuilder = battleEventService.doEvent(battleId, eventMsgBuilder);

        // 推送给本场战斗中的所有对手玩家
        battleEventService.pushEventMsgListToOneBattlePlayer(opponentUserId, eventMsgListBuilder);
        // 响应认输事件回客户端
        BaseBattle.ConcedeResponse.Builder builder = BaseBattle.ConcedeResponse.newBuilder();
        builder.setEventList(eventMsgListBuilder);
        return super.buildResponseNetMsg(userId, Rpc.RpcNameEnum.Concede_VALUE, builder);
    }
}
