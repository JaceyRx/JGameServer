package com.jacey.game.battle.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.jacey.game.battle.manager.OnlineClientManager;
import com.jacey.game.battle.manager.SpringManager;
import com.jacey.game.battle.service.BattleEventService;
import com.jacey.game.battle.service.impl.BattleEventServiceImpl;
import com.jacey.game.common.annotation.MessageMethodMapping;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.LocalMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.*;
import com.jacey.game.db.service.BattleInfoService;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.PlayStateService;
import com.jacey.game.db.service.impl.BattleInfoServiceImpl;
import com.jacey.game.db.service.impl.BattleServerLoadBalanceServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Description: 对战房间管理。专门对对战相关请求进行二次分发
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class BattleRoomManagerActor extends BaseMessageActor {

    private BattleEventService battleEventService = SpringManager.getInstance().getBean(BattleEventServiceImpl.class);
    private BattleInfoService battleInfoService = SpringManager.getInstance().getBean(BattleInfoServiceImpl.class);
    private BattleServerLoadBalanceService battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceServiceImpl.class);
    private PlayStateService playStateService = SpringManager.getInstance().getBean(PlayStateService.class);

    public BattleRoomManagerActor() {
        super();
    }

    public BattleRoomManagerActor(String actionPackageName) {
        super(actionPackageName);
    }

    /**
     * 创建战场
     * @param remoteMessage  主逻辑服务器传输的消息对象
     */
    @MessageMethodMapping(value = { RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeBattleServerCreateNewBattle_VALUE }, isNet = true)
    public void noticeBattleServerCreateNewBattle(RemoteMessage remoteMessage) {
        RemoteServer.NoticeBattleServerCreateNewBattleRequest request = remoteMessage
                .getLite(RemoteServer.NoticeBattleServerCreateNewBattleRequest.class);
        RemoteServer.BattleRoomInfo battleRoomInfo = request.getBattleRoomInfo();   	// 对战房间信息
        CommonEnum.BattleTypeEnum battleType = battleRoomInfo.getBattleType();		// 对战类型
        String battleId = battleRoomInfo.getBattleId();					// 对战id
        List<Integer> userIds = battleRoomInfo.getUserIdsList();		// 对战双方userId
        switch (battleType.getNumber()) {
            case CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer_VALUE: {   // 1v1
                // 1.将当前battleId 添加到正在进行对战的对战的battleId list中。 Redis
                battleInfoService.addPlayingBattleId(battleId, battleType);
                // 2.创建BaseBattleActor【每个对战创建一个。用于通知初始化战场与加载对战处理Action】
                ActorRef baseBattleActor = context().actorOf(Props.create(BaseBattleActor.class, "com.jacey.game.battle.action.baseBattle"));
                // 3.添加到在线客户端
                OnlineClientManager.getInstance().addBattleActor(battleId, baseBattleActor, userIds);
                // 4.通知BaseBattleActor初始化战场
                LocalMessage localMessage = new LocalMessage(
                        LocalServer.LocalRpcNameEnum.LocalRpcBattleServerInitBattle_VALUE, battleRoomInfo);
                baseBattleActor.tell(localMessage, ActorRef.noSender());
                // 5.响应给mainLogicServer创建战斗成功信息
                RemoteServer.NoticeBattleServerCreateNewBattleResponse.Builder builder = RemoteServer.NoticeBattleServerCreateNewBattleResponse.newBuilder();
                builder.setBattleRoomInfo(battleRoomInfo);
                RemoteMessage remoteMsg = new RemoteMessage(
                        RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeBattleServerCreateNewBattle_VALUE, builder);
                sender().tell(remoteMsg, ActorRef.noSender());
                break;
            } default: {
                log.error("handle RemoteRpcNoticeBattleServerCreateNewBattle error, not support battleType = {}",
                        battleType);
                break;
            }
        }
    }

    /**
     * 获取当前所在对局的信息、投降认输、落子、
     * 客户端发现当前回合玩家超时未行动，请求服务器强制结束回合、确认可以开始游戏、
     * 超时未确认可以开始游戏，则强制开始游戏
     * @param message
     * @throws Exception
     */
    @MessageMethodMapping(value = { Rpc.RpcNameEnum.GetBattleInfo_VALUE, Rpc.RpcNameEnum.Concede_VALUE,
            Rpc.RpcNameEnum.PlacePieces_VALUE, Rpc.RpcNameEnum.ReadyToStartGame_VALUE}, isNet = true)
    public void proxyNetMessageInvoke(IMessage message) throws Exception {
        NetMessage netMessage = (NetMessage) message;
        int sessionId = netMessage.getSessionId();
        int userId = netMessage.getUserId();
        // 根据userId获取所属battleId
        String battleId = battleServerLoadBalanceService.getBattleUserIdToBattleId(userId);
        // 获取该对战所属 battleActor，一个对战绑定一个BaseBattleActor
        ActorRef battleActor = OnlineClientManager.getInstance().getBattleActor(battleId);
        if (battleActor == null) {
            // 若无法在本battleServer中找到对应的BattleActor，说明对战之前已创建，但因为之前负责的battleServer下线，而交由本服务器处理
            // 则需要为该战斗重新建立BattleActor
            CommonMsg.UserState userState = playStateService.getUserStateByUserId(userId);		// 获取用户状态
            CommonEnum.BattleTypeEnum battleType = userState.getBattleType();		// 获取对战类型
            switch (battleType.getNumber()) {
                case CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer_VALUE: {    // 1v1
                    battleActor = context().actorOf(
                            Props.create(BaseBattleActor.class, "com.jacey.game.battle.action.baseBattle"));
                    break;
                } default: {
                    log.error("proxyNetMessageInvoke error, recreate battleActor error, unsupport battleType = {}",
                            battleType);
                    NetMessage errorNetMsg = new NetMessage(netMessage.getRpcNum(), Rpc.RpcErrorCodeEnum.ServerError_VALUE);
                    sender().tell(errorNetMsg, ActorRef.noSender());
                    return;
                }
            }

            List<Integer> battleUserIds = battleInfoService.getOneBattleUserIds(battleId);
            OnlineClientManager.getInstance().addBattleActor(battleId, battleActor, battleUserIds);
        }
        // 存储SessionId与GatewayResponseActor的对应关系
        OnlineClientManager.getInstance().addSessionIdToGatewayResponseActor(sessionId, sender());
        // 转发给协议对应的子Action
        battleActor.tell(netMessage, sender());
    }
}
