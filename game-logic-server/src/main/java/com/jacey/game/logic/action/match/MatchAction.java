package com.jacey.game.logic.action.match;

import com.jacey.game.common.action.BaseMessageAction;
import com.jacey.game.common.annotation.MessageClassMapping;
import com.jacey.game.common.exception.RpcErrorException;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.entity.PlayStateEntity;
import com.jacey.game.db.service.PlayStateService;
import com.jacey.game.logic.service.MatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description: 匹配请求处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
@Component
@MessageClassMapping(Rpc.RpcNameEnum.Match_VALUE)
public class MatchAction extends BaseMessageAction {

    @Autowired
    private MatchService matchService;

    @Autowired
    private PlayStateService playStateService;

    @Override
    protected void LogRequest(IMessage requestMessage) throws Exception {
        NetMessage req = (NetMessage) requestMessage;
        log.info("【匹配请求】 userId = {}:\n{}", req.getUserId(), req.getProtobufText(CommonMsg.MatchRequest.class));
    }

    @Override
    protected void LogResponse(IMessage responseMessage) throws Exception {
        NetMessage resp = (NetMessage) responseMessage;
        log.info("【匹配响应】 userId = {}:\n{}", resp.getUserId(), resp.getProtobufText(CommonMsg.MatchResponse.class));
    }

    @Override
    protected IMessage doAction(IMessage requestMessage) throws Exception {
        NetMessage msg = (NetMessage) requestMessage;
        int userId = msg.getUserId();

        CommonMsg.MatchRequest req = msg.getLite(CommonMsg.MatchRequest.class);
        CommonEnum.BattleTypeEnum battleType = req.getBattleType();	    // 获取匹配类型

        // 获取玩家状态
        PlayStateEntity playStateEntity = playStateService.getPlayStateByUserId(userId);
        if (playStateEntity.getUserActionState() == CommonEnum.UserActionStateEnum.Matching_VALUE) {
            // 状态为匹配中 抛异常
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.MatchErrorMatching_VALUE);
        } else if (playStateEntity.getUserActionState() == CommonEnum.UserActionStateEnum.Playing_VALUE) {
            // 状态为对战中 抛异常
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.MatchErrorPlaying_VALUE);
        } else if (playStateEntity.getUserActionState() != CommonEnum.UserActionStateEnum.ActionNone_VALUE) {
            // 状态Not None 抛异常
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.MatchErrorOtherActionState_VALUE);
        }
        // 添加玩家到匹配队列中。
        if (matchService.addMatchPlayer(userId, battleType) == false) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.ServerError_VALUE);
        }
        // 生成响应
        CommonMsg.MatchResponse.Builder builder = CommonMsg.MatchResponse.newBuilder();

        return super.buildResponseNetMsg(userId, Rpc.RpcNameEnum.Match_VALUE, builder);
    }
}
