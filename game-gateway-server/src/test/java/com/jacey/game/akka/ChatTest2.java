package com.jacey.game.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.common.utils.DateTimeUtil;
import org.junit.Before;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class ChatTest2 {

    private ActorSystem system;
    /** Remote Actor */
    private ActorSelection remoteActor;
    /** Response */
    private ActorRef responseActor;

    private String akkaPath = "akka://chatServer_1@192.168.2.193:50000/user/chatServerActor";

    @Before
    public void before() {
        system = ActorSysContainer.getInstance().getSystem();
        remoteActor = ActorSysContainer.getInstance().getChatRemoteActor();
        responseActor = system.actorOf(Props.create(ResponseActor2.class));
    }

    /**
     * 初始化对战房间
     */
    public void initCreateChatRoom() {
//        RemoteServer.NoticeChatServerCreateNewBattleChatRoomRequest.Builder builder = RemoteServer.
//                NoticeChatServerCreateNewBattleChatRoomRequest.newBuilder();
//        RemoteServer.ChatRoomInfo.Builder chatRoomInfo = RemoteServer.ChatRoomInfo.newBuilder();
//        chatRoomInfo.setBattleId("1_231dd22649dc4f6fbdd07a7b226758a9");
//        chatRoomInfo.setChatRoomType(CommonEnum.ChatRoomTypeEnum.TwoPlayerBattleChatRoomType);
//        RemoteMessage remoteMessage = new RemoteMessage(RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeChatServerCreateNewBattleChatRoom_VALUE, builder);
//        remoteActor.tell(remoteMessage, responseActor);
    }

    public void sendJoinChatRoom() {
        CommonMsg.JoinChatRoomRequest.Builder builder = CommonMsg.JoinChatRoomRequest.newBuilder();
        builder.setChatRoomType(CommonEnum.ChatRoomTypeEnum.TwoPlayerBattleChatRoomType);
        NetMessage netMessage = new NetMessage(Rpc.RpcNameEnum.JoinChatRoom_VALUE, builder);
        // 设置sessionId
        netMessage.setSessionId(2);
        // 设置userId
        netMessage.setUserId(13);
        // 发送
        remoteActor.tell(netMessage, responseActor);
    }

    public void sendText() {
        CommonMsg.BattleChatTextSendRequest.Builder builder = CommonMsg.BattleChatTextSendRequest.newBuilder();
        builder.setBattleChatTextScope(CommonEnum.BattleChatTextScopeEnum.EveryoneScope); // 作用域
        builder.setChatRoomType(CommonEnum.ChatRoomTypeEnum.TwoPlayerBattleChatRoomType);  // 聊天室类型
        builder.setSendTimestamp(DateTimeUtil.getCurrentTimestamp()); // 发送时间
        builder.setText("hello");   // 发送文本
        NetMessage netMessage = new NetMessage(Rpc.RpcNameEnum.BattleChatText_VALUE, builder);
        // 设置sessionId
        netMessage.setSessionId(2);
        // 设置userId
        netMessage.setUserId(13);
        // 发送
        remoteActor.tell(netMessage, responseActor);
    }

    public static void main(String[] args) {
        ChatTest2 chatTest2 = new ChatTest2();
        chatTest2.before();
        chatTest2.sendJoinChatRoom();
        chatTest2.sendText();
        chatTest2.sendText();
        chatTest2.sendText();
    }

}
