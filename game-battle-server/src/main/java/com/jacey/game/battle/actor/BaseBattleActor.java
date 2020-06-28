package com.jacey.game.battle.actor;

import com.jacey.game.battle.manager.MessageManager;
import com.jacey.game.battle.manager.SpringManager;
import com.jacey.game.battle.service.BattleEventService;
import com.jacey.game.battle.service.impl.BattleEventServiceImpl;
import com.jacey.game.common.annotation.MessageMethodMapping;
import com.jacey.game.common.msg.LocalMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.LocalServer;
import com.jacey.game.common.proto3.RemoteServer;

import java.rmi.Remote;

/**
 * @Description: 用于专门处理1v1对战请求，一场战斗绑定一个
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class BaseBattleActor extends BaseMessageActor {

    private BattleEventService battleEventService = SpringManager.getInstance().getBean(BattleEventServiceImpl.class);

    public BaseBattleActor() {
        super();
    }

    public BaseBattleActor(String actionPackageName) {
        super(actionPackageName);
    }

    /**
     * battleServer通知初始化战斗
     * @param localMessage	本地通讯对象
     * @throws Exception
     */
    @MessageMethodMapping(LocalServer.LocalRpcNameEnum.LocalRpcBattleServerInitBattle_VALUE)
    public void initBattle(LocalMessage localMessage) throws Exception {
        RemoteServer.BattleRoomInfo battleRoomInfo = (RemoteServer.BattleRoomInfo) localMessage.getLite();
        // 战局初始化
        battleEventService.initBattle(battleRoomInfo);
        // 通知初始化对战聊天室
        RemoteServer.NoticeChatServerCreateNewBattleChatRoomRequest.Builder builder = RemoteServer
                .NoticeChatServerCreateNewBattleChatRoomRequest.newBuilder();

        RemoteServer.ChatRoomInfo.Builder chatRoomInfoBuilder = RemoteServer.ChatRoomInfo.newBuilder();
        chatRoomInfoBuilder.setChatRoomType(CommonEnum.ChatRoomTypeEnum.TwoPlayerBattleChatRoomType);
        chatRoomInfoBuilder.setBattleId(battleRoomInfo.getBattleId());
        // 设置对战聊天房间信息
        builder.setChatRoomInfo(chatRoomInfoBuilder);

        RemoteMessage remoteMessage = new RemoteMessage(
                RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeChatServerCreateNewBattleChatRoom_VALUE, builder);
        // 推送
        MessageManager.getInstance().noticeChatServerCreateNewBattleChatRoom(remoteMessage, context().self());
    }

    /**
     * 接收初始化聊天室信息
     */
    @MessageMethodMapping(RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeChatServerCreateNewBattleChatRoom_VALUE)
    public void onReceivedNoticeBattleServerCreateNewBattle(RemoteMessage remoteMessage) {
        // TODO
    }

}
