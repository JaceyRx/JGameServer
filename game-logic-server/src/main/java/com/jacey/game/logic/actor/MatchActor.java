package com.jacey.game.logic.actor;

import com.jacey.game.common.annotation.MessageMethodMapping;
import com.jacey.game.common.msg.LocalMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.LocalServer;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.db.service.PlayUserService;
import com.jacey.game.db.service.impl.PlayUserServiceImpl;
import com.jacey.game.logic.manager.MessageManager;
import com.jacey.game.logic.manager.SpringManager;
import com.jacey.game.logic.service.MatchService;
import com.jacey.game.logic.service.impl.MatchServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Description: 匹配处理Actor 接收消息并分发到下属action
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class MatchActor extends BaseMessageActor {

    private final LocalMessage localMsgMatch = new LocalMessage(LocalServer.LocalRpcNameEnum.LocalRpcLogicServerMatch_VALUE);
    private MatchService matchService = SpringManager.getInstance().getBean(MatchServiceImpl.class);
    private PlayUserService userService = SpringManager.getInstance().getBean(PlayUserServiceImpl.class);

    public MatchActor() {
        super();
    }

    public MatchActor(String actionPackageName) {
        super(actionPackageName);
    }

    /**
     * MatchActor启动时调用
     * 开启定时任务，每隔1秒发起匹配计算，查看是否有正在匹配的用户。有则通知battle服务器创建战场。
     * battle创建战场完成后，通知主logic 服务器，然后主logic服务器再通知对战用户双方
     */
    @Override
    public void preStart() {
        super.preStart();
        // logicServer循环通知自己进行匹配计算
        // 每隔1秒给自己发匹配消息，从而实现间隔1秒进行1次匹配计算
        super.schedule(0, 1, localMsgMatch);
    }

    /**
     * MatchActor 内部调用，用于定时执行匹配计算
     * @param localMessage
     * @throws Exception
     */
    @MessageMethodMapping(LocalServer.LocalRpcNameEnum.LocalRpcLogicServerMatch_VALUE)
    public void doMatch(LocalMessage localMessage) throws Exception {
        matchService.doMatch();
    }

    /**
     * 用于接收battle服务器创建战场成功后通知。logic服务器再通知客户端
     * @param remoteMsg
     * @throws Exception
     */
    @MessageMethodMapping(RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeBattleServerCreateNewBattle_VALUE)
    public void onReceivedNoticeBattleServerCreateNewBattle(RemoteMessage remoteMsg) throws Exception {
        // 对战创建响应
        RemoteServer.NoticeBattleServerCreateNewBattleResponse response = remoteMsg
                .getLite(RemoteServer.NoticeBattleServerCreateNewBattleResponse.class);

        // 获取对战房间信息
        RemoteServer.BattleRoomInfo battleRoomInfo = response.getBattleRoomInfo();
        // 获取对战玩家信息
        List<Integer> userIds = battleRoomInfo.getUserIdsList();
        // 匹配结果消息体构建
        CommonMsg.MatchResultPush.Builder pushBuilder = CommonMsg.MatchResultPush.newBuilder();
        pushBuilder.setIsSuccess(true);	                            // 匹配成功
        pushBuilder.setBattleType(battleRoomInfo.getBattleType());	// 匹配类型
        pushBuilder.setBattleId(battleRoomInfo.getBattleId());		// battleId
        // 玩家简略信息列表（按行动顺序排列）
        for (int userId : userIds) {
            pushBuilder.addUserBriefInfos(userService.getUserBriefInfoByUserId(userId));
        }
        NetMessage netMsg = new NetMessage(Rpc.RpcNameEnum.MatchResultPush_VALUE, pushBuilder);
        for (int userId : userIds) {
            // 通知对战双方匹配结果
            MessageManager.getInstance().sendNetMsgToOneUser(userId, netMsg, CommonMsg.MatchResultPush.class);
        }
    }


}
