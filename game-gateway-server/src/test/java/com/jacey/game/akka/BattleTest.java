package com.jacey.game.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.*;
import org.junit.Before;

import java.util.Arrays;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class BattleTest {

    private ActorSystem system;
    /** Remote Actor */
    private ActorSelection remoteActor;
    /** Response */
    private ActorRef responseActor;

    private String akkaPath = "akka://chatServer_1@192.168.2.193:50000/user/chatServerActor";

    @Before
    public void before() {
        system = ActorSysContainer.getInstance().getSystem();
        remoteActor = ActorSysContainer.getInstance().getBattleRemoteActor();
        responseActor = system.actorOf(Props.create(ResponseActor1.class));
    }

    public void initBattle() {
        RemoteServer.NoticeBattleServerCreateNewBattleRequest.Builder request = RemoteServer.NoticeBattleServerCreateNewBattleRequest.newBuilder();
        RemoteServer.BattleRoomInfo.Builder battleRoomInfo = RemoteServer.BattleRoomInfo.newBuilder();
        battleRoomInfo.addAllUserIds(Arrays.asList(22, 23));
        battleRoomInfo.setBattleType(CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer);
        battleRoomInfo.setBattleId("xxxx");
        request.setBattleRoomInfo(battleRoomInfo);
        RemoteMessage remoteMessage = new RemoteMessage(RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeBattleServerCreateNewBattle_VALUE, request);
        remoteActor.tell(remoteMessage, responseActor);
    }

    /**
     * 获取对战信息测试
     */
    public void getBattleInfo() {
        BaseBattle.GetBattleInfoRequest.Builder builder = BaseBattle.GetBattleInfoRequest.newBuilder();
        NetMessage netMessage = new NetMessage(Rpc.RpcNameEnum.GetBattleInfo_VALUE, builder);
        // 设置sessionId
        netMessage.setSessionId(1);
        // 设置userId
        netMessage.setUserId(26);
        // 发送
        remoteActor.tell(netMessage, responseActor);
    }

    public void readToStarTest() {
        BaseBattle.ReadyToStartGameResponse.Builder builder = BaseBattle.ReadyToStartGameResponse.newBuilder();
        NetMessage netMessage = new NetMessage(Rpc.RpcNameEnum.ReadyToStartGame_VALUE, builder);
        // 设置sessionId
        netMessage.setSessionId(1);
        // 设置userId
        netMessage.setUserId(21);
        // 发送
        remoteActor.tell(netMessage, responseActor);
    }

    public void placePiecesTest() {
        BaseBattle.PlacePiecesRequest.Builder request = BaseBattle.PlacePiecesRequest.newBuilder();
        request.setLastEventNum(1);
        request.setIndex(0);
        NetMessage netMessage = new NetMessage(Rpc.RpcNameEnum.PlacePieces_VALUE, request);
        // 设置sessionId
        netMessage.setSessionId(1);
        // 设置userId
//        netMessage.setUserId(19);
        netMessage.setUserId(22);
        // 发送
        remoteActor.tell(netMessage, responseActor);
    }


    public static void main(String[] args) {
        BattleTest chatTest2 = new BattleTest();
        chatTest2.before();
//        chatTest2.initBattle();
        chatTest2.readToStarTest();
//        chatTest2.placePiecesTest();
    }

}
