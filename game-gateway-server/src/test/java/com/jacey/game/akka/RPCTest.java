package com.jacey.game.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.jacey.game.GatewayServerApplicationTest;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.RemoteServer;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class RPCTest {

    private ActorSystem system;
    /** Remote Actor */
    private ActorSelection remoteActor;

    private String akkaPath = "akka://battleServer_1@192.168.56.1:40000/user/battleServerActor";

    @Before
    public void before() {
        system = ActorSystem.create("test");
        remoteActor = system.actorSelection(akkaPath);
    }

    @Test
    public void noticeBattleServerCreateNewBattleTest() {
        RemoteServer.NoticeBattleServerCreateNewBattleRequest.Builder builder = RemoteServer.NoticeBattleServerCreateNewBattleRequest.newBuilder();
        RemoteServer.BattleRoomInfo.Builder battleRoomInfoBuilder = RemoteServer.BattleRoomInfo.newBuilder();
        battleRoomInfoBuilder.setBattleId("xxxxxxxxxxxx");
        battleRoomInfoBuilder.setBattleType(CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer);
        List list = new ArrayList();
        list.add(12);
        list.add(13);
        battleRoomInfoBuilder.addAllUserIds(list);
        builder.setBattleRoomInfo(battleRoomInfoBuilder);
        RemoteMessage remoteMessage = new RemoteMessage(
                RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeBattleServerCreateNewBattle_VALUE, builder);
        System.out.println("============"+builder.toString());
        remoteActor.tell(remoteMessage, ActorRef.noSender());
    }

    public static void main(String[] args) {
        RPCTest rpcTest = new RPCTest();
        rpcTest.before();
        rpcTest.noticeBattleServerCreateNewBattleTest();
    }
}
