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
 * @Description: 落子
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
@Component
@MessageClassMapping(value = Rpc.RpcNameEnum.PlacePieces_VALUE, isNet = false)
public class PlacePiecesAction extends BaseMessageAction {

    @Autowired
    private BattleServerLoadBalanceService battleServerLoadBalanceService;
    @Autowired
    private BattleInfoService battleInfoService;
    @Autowired
    private BattleEventService battleEventService;

    @Override
    protected void LogRequest(IMessage requestMessage) throws Exception {
        NetMessage req = (NetMessage) requestMessage;
        log.info("【落子请求】 userId = {}:\n{}", req.getUserId(), req.getProtobufText(BaseBattle.PlacePiecesRequest.class));
    }

    @Override
    protected void LogResponse(IMessage responseMessage) throws Exception {
        NetMessage resp = (NetMessage) responseMessage;
        log.info("【落子响应】 userId = {}:\n{}", resp.getUserId(), resp.getProtobufText(BaseBattle.PlacePiecesResponse.class));
    }

    @Override
    protected IMessage doAction(IMessage requestMessage) throws Exception {
        NetMessage msg = (NetMessage) requestMessage;
        int userId = msg.getUserId();
        BaseBattle.PlacePiecesRequest req = msg.getLite(BaseBattle.PlacePiecesRequest.class);
        // 获取对局中上一个事件编号，以此可知客户端是否已同步最新对局信息
        int inputLastEventNum = req.getLastEventNum();
        int inputIndex = req.getIndex();  // 落在哪个位置

        String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
        if (battleId == null) {
            // 对战不存在
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.UserNotInBattle_VALUE);
        }

        // 获取未准备userIds
        Set<Integer> notReadyUserIds = battleInfoService.getOneBattleNotReadyUserIds(battleId);
        if (notReadyUserIds != null && notReadyUserIds.size() > 0) {
            // 游戏尚未开始（需双方都确认ReadyToStartGame）
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.BattleNotStart_VALUE);
        }
        // 获取当前回合信息
        BaseBattle.CurrentTurnInfo currentTurnInfo = battleInfoService.getBattleCurrentTurnInfo(battleId);
        if (currentTurnInfo.getUserId() != userId) {
            // 不是该玩家的回合
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.IsNotUserTurn_VALUE);
        }
        // 获取当前对战上一个已发生事件的eventNum
        int lastEventNum = battleInfoService.getLastEventNum(battleId);
        log.info("lastEventNum: {}", lastEventNum);
        if (lastEventNum != inputLastEventNum) {
            // 客户端请求对战操作时附带的lastEventNum错误，说明发生丢包
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.InputLastEventNumError_VALUE);
        }
        if (inputIndex < 0 || inputIndex > 8) {
            // 请求落子错误，要落子的位置非法
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.PlacePiecesErrorIndexError_VALUE);
        }
        // 获取当前对战要落子区域棋盘信息
        int oneCellInfo = battleInfoService.getOneBattleCellInfo(battleId, inputIndex);
        if (oneCellInfo != 0) {
            // 请求落子错误，要落子的位置已经有棋子
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.PlacePiecesErrorIndexIsNotEmpty_VALUE);
        }
        // 这里必须在doGameOverEvent之前取得，否则就会因为redis中对战相关信息被清除而无法取得
        // 获取对手id
        int opponentUserId = battleInfoService.getOneUserOneOpponentUserId(battleId, userId);
        // 构造落子事件
        BaseBattle.PlacePiecesEvent.Builder placePiecesEventBuilder = BaseBattle.PlacePiecesEvent.newBuilder();
        placePiecesEventBuilder.setUserId(userId);		// 哪个玩家在操作
        placePiecesEventBuilder.setIndex(inputIndex);   // 落在棋盘哪个位置
        // 构造一个事件消息
        BaseBattle.EventMsg.Builder eventMsgBuilder = battleEventService.buildOneEvent(battleId, BaseBattle.EventTypeEnum.EventTypePlacePieces,
                placePiecesEventBuilder);
        // 执行事件
        BaseBattle.EventMsgList.Builder eventMsgListBuilder = battleEventService.doEvent(battleId, eventMsgBuilder);
        // 将产生的一系列事件推送给对手
        battleEventService.pushEventMsgListToOneBattlePlayer(opponentUserId, eventMsgListBuilder);
        // 生成落子响应.将产生的一系列事件返回客户端
        BaseBattle.PlacePiecesResponse.Builder builder = BaseBattle.PlacePiecesResponse.newBuilder();
        builder.setEventList(eventMsgListBuilder);
        return super.buildResponseNetMsg(userId, Rpc.RpcNameEnum.PlacePieces_VALUE, builder);
    }
}
