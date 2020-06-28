package com.jacey.game.logic.action.match;

import com.jacey.game.common.action.BaseMessageAction;
import com.jacey.game.common.annotation.MessageClassMapping;
import com.jacey.game.common.exception.RpcErrorException;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.CommonEnum.UserActionStateEnum;
import com.jacey.game.common.proto3.CommonEnum.BattleTypeEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.entity.PlayStateEntity;
import com.jacey.game.db.service.PlayStateService;
import com.jacey.game.logic.service.MatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description: 取消匹配
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
@Component
@MessageClassMapping(Rpc.RpcNameEnum.CancelMatch_VALUE)
public class CancelMatchAction extends BaseMessageAction {

    @Autowired
    private MatchService matchService;

    @Autowired
    private PlayStateService playStateService;

    @Override
    protected void LogRequest(IMessage requestMessage) throws Exception {
        NetMessage req = (NetMessage) requestMessage;
        log.info("【取消匹配请求】 userId = {}:\n{}", req.getUserId(),
                req.getProtobufText(CommonMsg.CancelMatchRequest.class));
    }

    @Override
    protected void LogResponse(IMessage responseMessage) throws Exception {
        NetMessage resp = (NetMessage) responseMessage;
        log.info("【取消匹配请求】 userId = {}:\n{}", resp.getUserId(),
                resp.getProtobufText(CommonMsg.CancelMatchResponse.class));
    }

    @Override
    protected IMessage doAction(IMessage requestMessage) throws Exception {
        NetMessage msg = (NetMessage) requestMessage;
        int userId = msg.getUserId();

        PlayStateEntity playStateEntity = playStateService.getPlayStateByUserId(userId);
        UserActionStateEnum actionState = UserActionStateEnum.forNumber(playStateEntity.getUserActionState());
        if (actionState == UserActionStateEnum.Playing) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.CancelMatchErrorPlaying_VALUE);
        } else if (actionState != UserActionStateEnum.Matching) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.CancelMatchErrorNotMatching_VALUE);
        }

        if (matchService.removeMatchPlayer(userId, BattleTypeEnum.forNumber(playStateEntity.getBattleType())) == false) {
            throw new RpcErrorException(Rpc.RpcErrorCodeEnum.ServerError_VALUE);
        }

        CommonMsg.CancelMatchResponse.Builder builder = CommonMsg.CancelMatchResponse.newBuilder();
        return super.buildResponseNetMsg(userId, Rpc.RpcNameEnum.CancelMatch_VALUE, builder);
    }
}
