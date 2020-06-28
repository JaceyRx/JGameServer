package com.jacey.game.chat.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.jacey.game.chat.manager.OnlineClientManager;
import com.jacey.game.chat.manager.SpringManager;
import com.jacey.game.common.annotation.MessageMethodMapping;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.common.proto3.RemoteServer.NoticeChatServerCreateNewBattleChatRoomRequest;
import com.jacey.game.common.proto3.RemoteServer.NoticeChatServerCreateNewBattleChatRoomResponse;
import com.jacey.game.common.proto3.RemoteServer.RemoteRpcNameEnum;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.impl.BattleServerLoadBalanceServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 用于对战聊天请求的接收与分发
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class ChatRoomMangerActor extends BaseMessageActor {

    private BattleServerLoadBalanceService battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceServiceImpl.class);

    public ChatRoomMangerActor() {
        super();
    }

    public ChatRoomMangerActor(String actionPackageName) {
        super(actionPackageName);
    }


    /**
     * 创建战场
     * @param remoteMessage  主逻辑服务器传输的消息对象
     */
    @MessageMethodMapping(value = { RemoteRpcNameEnum.RemoteRpcNoticeChatServerCreateNewBattleChatRoom_VALUE }, isNet = true)
    public void noticeChatServerCreateNewBattleChatRoom(RemoteMessage remoteMessage) {
        NoticeChatServerCreateNewBattleChatRoomRequest request = remoteMessage
                .getLite(NoticeChatServerCreateNewBattleChatRoomRequest.class);
        RemoteServer.ChatRoomInfo chatRoomInfo = request.getChatRoomInfo();
        CommonEnum.ChatRoomTypeEnum chatRoomType = chatRoomInfo.getChatRoomType();
        String battleId = chatRoomInfo.getBattleId();  // 对战id
        // 根据不同聊天室类型进行分发
        switch (chatRoomType.getNumber()) {
            case CommonEnum.ChatRoomTypeEnum.TwoPlayerBattleChatRoomType_VALUE: {
                /** 双人对战聊天室 */
                // 1.创建一个BaseBattleChatRoomActor
                ActorRef baseBattleChatRoomActor = context().actorOf(Props.create(BaseBattleChatRoomActor.class));
                // 2.添加 BaseBattleChatRoomActor、BattleId添加到 OnlineClientManager中
                OnlineClientManager.getInstance().addBattleChatRoomActor(battleId ,baseBattleChatRoomActor);
                // 3.响应
                NoticeChatServerCreateNewBattleChatRoomResponse.Builder battleChatRoomResponse = NoticeChatServerCreateNewBattleChatRoomResponse.newBuilder();
                RemoteMessage remoteMsg = new RemoteMessage(RemoteRpcNameEnum.RemoteRpcNoticeChatServerCreateNewBattleChatRoom_VALUE, battleChatRoomResponse);
                sender().tell(remoteMsg, ActorRef.noSender());
                log.info("对战聊天室初始化完成....");
                break;
            } default: {
                log.error("handle RemoteRpcNoticeChatServerCreateNewBattleChatRoom error, not support chatRoomType = {}",
                        chatRoomType);
                break;
            }
        }
    }

    /**
     * 加入对战聊天室/对战聊天室文本推送
     * @param message
     * @throws Exception
     */
    @MessageMethodMapping(value = { Rpc.RpcNameEnum.JoinChatRoom_VALUE,
            Rpc.RpcNameEnum.BattleChatText_VALUE }, isNet = true)
    public void proxyNetMessageInvoke(IMessage message) throws Exception {
        NetMessage netMessage = (NetMessage) message;
        // 获取sessionId 用于与GatewayResponseActor绑定
        int sessionId = netMessage.getSessionId();
        int userId = netMessage.getUserId();         // 获取userId用于获得battleId
        String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
        // 获取BaseBattleChatRoomActor
        ActorRef baseBattleChatRoomActor = OnlineClientManager.getInstance().getBattleChatRoomActor(battleId);
        // 如果BaseBattleChatRoomActor 为空。则重新初始化聊天室
        if (baseBattleChatRoomActor == null) {
            // 若无法在本chatServer中找到对应的BaseBattleChatRoomActor，说明对战聊天之前已创建，但因为之前负责的ChatServer下线，而交由本服务器处理
            // TODO: baseBattleChatRoomActor == null 未做处理
            NetMessage errorMsg = new NetMessage(netMessage.getRpcNum(), Rpc.RpcErrorCodeEnum.ServerError_VALUE);
            sender().tell(errorMsg, ActorRef.noSender());
        }
        // 存储SessionId与GatewayResponseActor的对应关系
        OnlineClientManager.getInstance().addSessionIdToGatewayResponseActor(sessionId, sender());
        // 通知BaseBattleChatRoomActor对消息进行处理
        baseBattleChatRoomActor.tell(netMessage, sender());
    }
}
