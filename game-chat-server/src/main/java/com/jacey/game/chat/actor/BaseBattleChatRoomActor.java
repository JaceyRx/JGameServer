package com.jacey.game.chat.actor;

import akka.actor.ActorRef;
import com.jacey.game.chat.manager.MessageManager;
import com.jacey.game.chat.manager.SpringManager;
import com.jacey.game.common.annotation.MessageMethodMapping;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.service.BattleInfoService;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.impl.BattleInfoServiceImpl;
import com.jacey.game.db.service.impl.BattleServerLoadBalanceServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/**
 * @Description: 用于对战聊天请求的处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class BaseBattleChatRoomActor extends BaseMessageActor{

    private BattleServerLoadBalanceService battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceServiceImpl.class);
    private BattleInfoService battleInfoService = SpringManager.getInstance().getBean(BattleInfoServiceImpl.class);

    public BaseBattleChatRoomActor() {
        super();
    }

    public BaseBattleChatRoomActor(String actionPackageName) {
        super(actionPackageName);
    }

    /**
     * 加入对战聊天室
     * @param message
     */
    @MessageMethodMapping(value = Rpc.RpcNameEnum.JoinChatRoom_VALUE, isNet = false)
    public IMessage joinChatRoom(IMessage message) {
        CommonMsg.JoinChatRoomResponse.Builder response = CommonMsg.JoinChatRoomResponse.newBuilder();
        NetMessage netMessage = new NetMessage(Rpc.RpcNameEnum.JoinChatRoom_VALUE, response);
//        sender().tell(netMessage, ActorRef.noSender());
        return netMessage;
    }

    /**
     * 聊天信息转发
     */
    @MessageMethodMapping(value = Rpc.RpcNameEnum.BattleChatText_VALUE, isNet = false)
    public IMessage battleChatText(IMessage message) {
        NetMessage netMessage = (NetMessage) message;
        CommonMsg.BattleChatTextSendRequest request = netMessage.getLite(CommonMsg.BattleChatTextSendRequest.class);
        CommonEnum.ChatRoomTypeEnum chatRoomTypeEnum = request.getChatRoomType();
        int userId = netMessage.getUserId();
        // 根据获取对战id，然后根据对战id获取该对战的所有UserId
        String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
        if (battleId == null) {
            // 对战聊天发送失败，未加入对战
            NetMessage resNetMsg = new NetMessage(Rpc.RpcNameEnum.BattleChatText_VALUE,
                    Rpc.RpcErrorCodeEnum.BattleChatTextErrorNotJoinBattle_VALUE);
            return resNetMsg;
        }
        // 根据发送消息的作用域。发送给对应的玩家
        switch (chatRoomTypeEnum.getNumber()) {
            case CommonEnum.ChatRoomTypeEnum.TwoPlayerBattleChatRoomType_VALUE: {
                // 2人对战聊天室
                List<Integer> userIds = battleInfoService.getOneUserAllOpponentUserIds(battleId, userId);  // 获取所有对手id
//                userIds.remove(userId);
                // 构造推送消息体
                CommonMsg.BattleChatTextPush.Builder builder = CommonMsg.BattleChatTextPush.newBuilder();
                builder.setSenderUserId(userId);                        // 发送方UserId
                builder.setBattleChatTextScope(request.getBattleChatTextScope()); // 文本作用域
                builder.setText(request.getText());                     // 发送文本
                builder.setSendTimestamp(request.getSendTimestamp());  // 发送时间
                NetMessage netMsg = new NetMessage(Rpc.RpcNameEnum.BattleChatTextPush_VALUE, builder);
                // 推送
                MessageManager.getInstance().sendNetMsgToOneUser(userIds.get(0), netMsg, CommonMsg.BattleChatTextPush.class);
                break;
            } default: {
                log.error("netRpc handle BattleChatText error， not support ChatRoomType = {}",
                        chatRoomTypeEnum);
                break;
            }
        }
        CommonMsg.BattleChatTextSendResponse.Builder response = CommonMsg.BattleChatTextSendResponse.newBuilder();
        NetMessage resNetMsg = new NetMessage(Rpc.RpcNameEnum.BattleChatText_VALUE, response);
        return resNetMsg;
    }

}
