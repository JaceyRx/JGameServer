package com.jacey.game.battle.service.impl;

import com.google.protobuf.GeneratedMessageV3;
import com.jacey.game.battle.manager.MessageManager;
import com.jacey.game.battle.manager.OnlineClientManager;
import com.jacey.game.battle.service.BattleEventService;
import com.jacey.game.common.exception.RpcErrorException;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.BaseBattle;
import com.jacey.game.common.proto3.BaseBattle.EventMsg;
import com.jacey.game.common.proto3.BaseBattle.EventMsgList;
import com.jacey.game.common.proto3.BaseBattle.EventTypeEnum;
import com.jacey.game.common.proto3.BaseBattle.StartTurnEvent;
import com.jacey.game.common.proto3.BaseBattle.CurrentTurnInfo;
import com.jacey.game.common.proto3.BaseBattle.PlacePiecesEvent;
import com.jacey.game.common.proto3.BaseBattle.GameOverEvent;
import com.jacey.game.common.proto3.BaseBattle.GameOverReasonEnum;
import com.jacey.game.common.proto3.BaseBattle.EndTurnEvent;
import com.jacey.game.common.proto3.BaseBattle.BattleEventMsgListPush;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonEnum.BattleTypeEnum;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.common.utils.DateTimeUtil;
import com.jacey.game.db.entity.BattleRecordEntity;
import com.jacey.game.db.service.BattleInfoService;
import com.jacey.game.db.service.BattleRecordService;
import com.jacey.game.db.service.PlayStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description: 对战事件处理实现
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Service
@Slf4j
public class BattleEventServiceImpl implements BattleEventService {


    //棋盘
    private static final List<Integer> initCellInfo = new ArrayList<Integer>();

    static {
        for (int i = 0; i < 9; ++i) {
            initCellInfo.add(0);
        }
    }

    @Autowired
    private BattleInfoService battleInfoService;
    @Autowired
    private BattleRecordService battleRecordService;
    @Autowired
    private PlayStateService playStateService;

    /**
     *  初始化战场
     * @param battleRoomInfo
     */
    @Override
    public void initBattle(RemoteServer.BattleRoomInfo battleRoomInfo) {
        long currentTimestamp = DateTimeUtil.getCurrentTimestamp();
        String battleId = battleRoomInfo.getBattleId();         // 获取对战id
        List<Integer> userIds = battleRoomInfo.getUserIdsList();    // 获取对战双方userId
        // 临时存储当前对战的玩家Id
        battleInfoService.initOneBattleUserIds(battleId, userIds);
        // 初始化棋盘信息
        battleInfoService.initAllBattleCellInfo(battleId, initCellInfo);
        // 设置对战开始时间 Redis
        battleInfoService.setOneBattleStartTimestamp(battleId, currentTimestamp);
        // 初始化一个未准备就绪的UserIds
        battleInfoService.initOneBattleNotReadyUserIds(battleId, userIds);
    }

    /**
     * 第一回合（0回合特殊处理）
     * @param battleId
     */
    @Override
    public void startFirstTurn(String battleId) throws Exception {
        // 1.获取对战双方userId
        List<Integer> userIds = battleInfoService.getOneBattleUserIds(battleId);
        // 2.构造当前回合信息。并存储到Redis中
        CurrentTurnInfo.Builder builder = CurrentTurnInfo.newBuilder();
        builder.setUserId(userIds.get(userIds.size() - 1));		// 设置当前回合userId	（后手userId） (2-1=1)
        builder.setTurnCount(0);		// 设置当前0回合
        battleInfoService.setBattleCurrentTurnInfo(battleId, builder.build());
        // 3.构造回合开始事件
        StartTurnEvent.Builder startTurnEventBuilder = StartTurnEvent.newBuilder();
        // 4.执行开始事件
        EventMsg.Builder eventMsgBuilder = buildOneEvent(battleId, EventTypeEnum.EventTypeStartTurn,
                startTurnEventBuilder);
        // 5.将执行事件产生的事件消息列表推送给所有玩家
        EventMsgList.Builder eventMsgListBuilder = doEvent(battleId, eventMsgBuilder);	// 执行事件，并返回一系列后续事件
        pushEventMsgListToAllBattlePlayers(battleId, eventMsgListBuilder);
    }

    @Override
    public BaseBattle.EventMsgList.Builder doEvent(String battleId, BaseBattle.EventMsg.Builder firstEventBuilder) throws Exception {
        // 用于存储执行过的事件消息。
        EventMsgList.Builder resultEventMsgList = EventMsgList.newBuilder();
        // 1.构造一个用于存储未执行事件的todoList。并添加 firstEventBuilder
        List<EventMsg.Builder> todoEventList = new ArrayList<>();
        todoEventList.add(firstEventBuilder);
        // 2.声明一个nextEventList。用于存储接下来执行事件后产生的后续事件List
        List<EventMsg.Builder> nextEventList = null;
        // 3.while循环不断消费 todoList
        while (todoEventList.size() > 0) {
            EventMsg.Builder firstTodoEventBuilder = todoEventList.get(0);
            // 获取当前对战下一个事件编号（自增Redis键）
            int eventNum = battleInfoService.addAndGetNextAvailableEventNum(battleId);
            firstTodoEventBuilder.setEventNum(eventNum);
            int todoEventType = firstTodoEventBuilder.getEventTypeValue();
            // 根据不同事件类型执行不同的动作
            switch (todoEventType) {
                case EventTypeEnum.EventTypeGameOver_VALUE: {
                    /** 游戏结束事件 */
                    nextEventList = doGameOverEvent(battleId, firstTodoEventBuilder);
                    break;
                } case EventTypeEnum.EventTypeStartTurn_VALUE: {
                    /** 回合开始事件 */
                    nextEventList = doStartTurnEvent(battleId, firstTodoEventBuilder);
                    break;
                } case EventTypeEnum.EventTypeEndTurn_VALUE: {
                    /** 回合结束事件 */
                    nextEventList = doEndTurnEvent(battleId, firstTodoEventBuilder);
                    break;
                } case EventTypeEnum.EventTypePlacePieces_VALUE: {
                    /** 落子事件 */
                    nextEventList = doPlacePiecesEvent(battleId, firstTodoEventBuilder);
                    break;
                } default: {
                    log.error("【事件执行异常】 not support eventType = {}", todoEventType);
                    throw new RpcErrorException(Rpc.RpcErrorCodeEnum.ServerError_VALUE);
                }
            }
            // firstTodoEvent事件执行完成。从todoList中移除.并添加到要返回的EventMsgList中
            todoEventList.remove(0);
            log.error("redis获取：{} eventNum = {} eventType = {}", eventNum, firstTodoEventBuilder.getEventNum(), firstTodoEventBuilder.getEventType());
            resultEventMsgList.addMsgList(firstTodoEventBuilder.build());
            // 存储已执行的firstTodoEvent事件。
            battleInfoService.addOneBattleEvent(battleId, firstTodoEventBuilder.build());
            // 将不同事件执行产生的nextEventMsgList添加到todoList中。
            if (nextEventList != null) {
                todoEventList.addAll(0, nextEventList);
            }
        }
        return resultEventMsgList;
    }

    /**
     * 构造事件消息
     * @param battleId
     * @param eventType
     * @param eventBuilder
     * @return
     * @throws RpcErrorException
     */
    @Override
    public BaseBattle.EventMsg.Builder buildOneEvent(String battleId, BaseBattle.EventTypeEnum eventType, GeneratedMessageV3.Builder eventBuilder) throws RpcErrorException {
        EventMsg.Builder builder = EventMsg.newBuilder();
        builder.setEventType(eventType);		// 设置事件类型

        switch (eventType.getNumber()) {
            case EventTypeEnum.EventTypeGameOver_VALUE: {  // 对战结束
                builder.setGameOverEvent((GameOverEvent.Builder) eventBuilder);
                break;
            } case EventTypeEnum.EventTypeStartTurn_VALUE: { // 回合开始
                builder.setStartTurnEvent((StartTurnEvent.Builder) eventBuilder);
                break;
            } case EventTypeEnum.EventTypeEndTurn_VALUE: {  // 回合结束
                builder.setEndTurnEvent((EndTurnEvent.Builder) eventBuilder);
                break;
            } case EventTypeEnum.EventTypePlacePieces_VALUE: {  // 落子
                builder.setPlacePiecesEvent((PlacePiecesEvent.Builder) eventBuilder);
                break;
            } default: {
                log.error("buildOneEvent error, unsupport eventType = {}", eventType);
                throw new RpcErrorException(Rpc.RpcErrorCodeEnum.ServerError_VALUE);
            }
        }

        return builder;
    }

    @Override
    public void pushEventMsgListToAllBattlePlayers(String battleId, BaseBattle.EventMsgList.Builder eventMsgListBuilder) {
        BattleEventMsgListPush.Builder pushBuilder = BattleEventMsgListPush.newBuilder();
        pushBuilder.setEventMsgList(eventMsgListBuilder);
        NetMessage netMsg = new NetMessage(Rpc.RpcNameEnum.BattleEventMsgListPush_VALUE, pushBuilder);
        List<Integer> userIds = battleInfoService.getOneBattleUserIds(battleId);
        for (int userId : userIds) {
            MessageManager.getInstance().sendNetMsgToOneUser(userId, netMsg, BattleEventMsgListPush.class);
        }
    }

    @Override
    public void pushEventMsgListToOneBattlePlayer(int userId, BaseBattle.EventMsgList.Builder eventMsgListBuilder) {
        BattleEventMsgListPush.Builder pushBuilder = BattleEventMsgListPush.newBuilder();
        pushBuilder.setEventMsgList(eventMsgListBuilder);
        NetMessage netMsg = new NetMessage(Rpc.RpcNameEnum.BattleEventMsgListPush_VALUE, pushBuilder);
        MessageManager.getInstance().sendNetMsgToOneUser(userId, netMsg, BattleEventMsgListPush.class);
    }

    @Override
    public void pushEventMsgListToAllOpponentBattlePlayers(String battleId, int userId, BaseBattle.EventMsgList.Builder eventMsgListBuilder) {
        BattleEventMsgListPush.Builder pushBuilder = BattleEventMsgListPush.newBuilder();
        pushBuilder.setEventMsgList(eventMsgListBuilder);
        NetMessage netMsg = new NetMessage(Rpc.RpcNameEnum.BattleEventMsgListPush_VALUE, pushBuilder);
        List<Integer> allOpponentUserIds = battleInfoService.getOneUserAllOpponentUserIds(battleId, userId);
        for (int oneOpponentUserId : allOpponentUserIds) {
            MessageManager.getInstance().sendNetMsgToOneUser(oneOpponentUserId, netMsg, BattleEventMsgListPush.class);
        }
    }


    /**
     * 游戏结束事件 handle Method
     * @param battleId
     * @param eventBuilder
     * @return
     */
    private List<EventMsg.Builder> doGameOverEvent(String battleId, EventMsg.Builder eventBuilder) throws Exception {
        GameOverEvent gameOverEvent = eventBuilder.getGameOverEvent();
        // 获取对战双方UserId List
        List<Integer> userIds = battleInfoService.getOneBattleUserIds(battleId);
        // 获取当前回合信息
        CurrentTurnInfo currentTurnInfo = battleInfoService.getBattleCurrentTurnInfo(battleId);
        // 对战记录数据归档存储Mysql
        BattleRecordEntity battleRecord = new BattleRecordEntity();
        battleRecord.setBattleType(BattleTypeEnum.BattleTypeTwoPlayer_VALUE);
        battleRecord.setBattleId(battleId);
        List<String> strUserIds = userIds.stream().map(e -> e.toString()).collect(Collectors.toList());
        battleRecord.setUserIdList(String.join(",", strUserIds));
        Date battleStartDate = DateTimeUtil.timestampToDate(battleInfoService.getOneBattleStartTimestamp(battleId));
        battleRecord.setBattleStartTimestamp(battleStartDate);
        battleRecord.setBattleEndTimestamp(new Date());
        battleRecord.setTurnCount(currentTurnInfo.getTurnCount());
        battleRecord.setWinnerUserId(gameOverEvent.getWinnerUserId());
        battleRecord.setGameOverReason(gameOverEvent.getGameOverReasonValue());
        battleRecordService.saveBattleRecord(battleRecord);

        // 取消玩家对战状态
        for (int userId : userIds) {
            playStateService.changeUserActionState(userId,
                    CommonEnum.UserActionStateEnum.ActionNone_VALUE,
                    BattleTypeEnum.NoneType_VALUE, battleId);
        }
        // 将battleId从正在对战中状态移除
        battleInfoService.removePlayingBattleId(battleId, BattleTypeEnum.BattleTypeTwoPlayer);
        // 清除临时数据
        battleInfoService.cleanOneBattleUserIds(battleId);
        battleInfoService.removeBattleCurrentTurnInfo(battleId);
        battleInfoService.cleanAllBattleCellInfo(battleId);
        battleInfoService.removeLastEventNum(battleId);
        battleInfoService.removeOneBattleStartTimestamp(battleId);
        battleInfoService.cleanOneBattleNotReadyUserIds(battleId);
        // 移除当前对战绑定的BaseBattleActor
        OnlineClientManager.getInstance().removeBattleActor(battleId, userIds);

        // 游戏结束没有后续事件
        return null;
    }

    /**
     * 回合开始事件 handle Method
     * @param battleId
     * @param eventBuilder
     * @return
     */
    private List<EventMsg.Builder> doStartTurnEvent(String battleId, EventMsg.Builder eventBuilder) throws Exception {
        StartTurnEvent.Builder startTurnEventBuilder = eventBuilder.getStartTurnEvent().toBuilder();
        // 获取上一回合信息。并重新设置回合信息
        CurrentTurnInfo.Builder currentTurnInfoBuilder = battleInfoService.getBattleCurrentTurnInfo(battleId).toBuilder();
        // 重设下一回合开始开始时间
        currentTurnInfoBuilder.setTurnStartTimestamp(DateTimeUtil.getCurrentTimestamp());
        // 获取对战双方userId
        List<Integer> userIds = battleInfoService.getOneBattleUserIds(battleId);
        // 获取上回合玩家UserId。并获取其执行顺序索引
        int index = userIds.indexOf(currentTurnInfoBuilder.getUserId());
        // 生成下一回合玩家执行顺序索引
        int nextTurnUserIndex = (index + 1) % userIds.size();
        // 获取该玩家userId。重设下一回合UserId
        currentTurnInfoBuilder.setUserId(userIds.get(nextTurnUserIndex));
        // 又轮到先手回合时，则回合数+1
        if (nextTurnUserIndex == 0) {
            currentTurnInfoBuilder.setTurnCount(currentTurnInfoBuilder.getTurnCount() + 1);
        }
        CurrentTurnInfo newCurrentTurnInfo = currentTurnInfoBuilder.build();
        // 保存下一个回合信息
        battleInfoService.setBattleCurrentTurnInfo(battleId, newCurrentTurnInfo);

        return null;
    }

    /**
     * 回合结束事件 handle Method
     * @param battleId
     * @param eventBuilder
     * @return
     */
    private List<EventMsg.Builder> doEndTurnEvent(String battleId, EventMsg.Builder eventBuilder) throws Exception {
        // 生成下一个事件为回合开始事件
        List<EventMsg.Builder> nextEventList = new ArrayList<EventMsg.Builder>();
        StartTurnEvent.Builder startTurnEventBuilder = StartTurnEvent.newBuilder();
        startTurnEventBuilder.setCurrentTurnInfo(battleInfoService.getBattleCurrentTurnInfo(battleId));
        EventMsg.Builder eventMsgBuilder = buildOneEvent(battleId, EventTypeEnum.EventTypeStartTurn, startTurnEventBuilder);
        nextEventList.add(eventMsgBuilder);
        return nextEventList;
    }

    /**
     * 落子事件 handle Method
     * @param battleId
     * @param eventBuilder
     * @return
     */
    private List<EventMsg.Builder> doPlacePiecesEvent(String battleId, EventMsg.Builder eventBuilder) throws RpcErrorException {
        List<EventMsg.Builder> nextEventList = new ArrayList<EventMsg.Builder>();
        PlacePiecesEvent placePiecesEvent = eventBuilder.getPlacePiecesEvent();
        int userId = placePiecesEvent.getUserId();
        int index = placePiecesEvent.getIndex();
        // getOneUserSeq() 获取某个玩家在某场战斗的行动顺序（先手为1，依次递增）
        int userSeq = battleInfoService.getOneUserSeq(battleId, userId);
        // 设置棋盘信息
        battleInfoService.setOneBattleCellInfo(battleId, index, userSeq);
        // 【游戏核心逻辑】检测游戏是否结束
        // checkAndGetWinnerUserSeq 执行落棋检测 -1表示战斗未完，0表示平局.其他为获胜方的行动顺序
        int winnerUserSeq = checkAndGetWinnerUserSeq(battleId, index);
        if (winnerUserSeq != -1) {
            // 检测到游戏结束，下一个事件为游戏结束事件
            GameOverEvent.Builder gameOverEventBuilder = GameOverEvent.newBuilder();
            if (winnerUserSeq == 0) {	// 平局
                gameOverEventBuilder.setWinnerUserId(0);	// 设置获胜方userId，平局为0
                gameOverEventBuilder.setGameOverReason(GameOverReasonEnum.GameOverDraw); // 游戏结束的原因：平局
            } else {
                int winnerUserId = battleInfoService.getOneUserIdBySeq(battleId, winnerUserSeq); // 根据行动顺序获取用户id
                gameOverEventBuilder.setWinnerUserId(winnerUserId); // 设置获胜方userId
                gameOverEventBuilder.setGameOverReason(GameOverReasonEnum.GameOverPlayerWin); //  游戏结束的原因
            }
            // 构造一个事件消息
            EventMsg.Builder eventMsgBuilder = buildOneEvent(battleId, EventTypeEnum.EventTypeGameOver,
                    gameOverEventBuilder);
            nextEventList.add(eventMsgBuilder);
        } else {
            // 游戏尚未结束，下一个事件为回合结束事件
            EndTurnEvent.Builder endTurnEventBuilder = EndTurnEvent.newBuilder();
            endTurnEventBuilder.setEndTurnUserId(userId);  	// 当前用户结束回合
            endTurnEventBuilder.setIsForceEndTurn(false);	// 是否因为超时，由系统强制结束回合
            // 构造一个事件消息
            EventMsg.Builder eventMsgBuilder = buildOneEvent(battleId, EventTypeEnum.EventTypeEndTurn,
                    endTurnEventBuilder);
            nextEventList.add(eventMsgBuilder);
        }
        return nextEventList;
    }

    /**
     * 【游戏核心逻辑】 检测游戏是否结束
     * @param justPlacePiecesIndex 刚刚落子的位置，只需检测该子上下斜向是否连成而胜利即可
     * @return -1表示战斗未完，0表示平局，除此之外的数字表示游戏因一方胜利而结束，返回获胜者的行动顺序
     */
    public int checkAndGetWinnerUserSeq(String battleId, int justPlacePiecesIndex) {
        boolean isWin = true;
        // 获取棋盘信息
        List<Integer> allBattleCellInfo = battleInfoService.getAllBattleCellInfo(battleId);
        int justPlacePiecesUserSeq = allBattleCellInfo.get(justPlacePiecesIndex);
        // 检测横行是否连成
        isWin = true;
        // 第一行第一个(row=0,index=0)
        // 第二行第一个(row=1,index=3)
        int rowNum = justPlacePiecesIndex / 3;	// 落子在第几行
        int thisRowStartIndex = rowNum * 3;		// 这行开始的那个位置索引
        for (int i = thisRowStartIndex; i < thisRowStartIndex + 3; ++i) {
            if (allBattleCellInfo.get(i) != justPlacePiecesUserSeq) {
                isWin = false;
                break;
            }
        }
        if (isWin == true) {
            return justPlacePiecesUserSeq;
        }
        // 检测竖列是否连成
        isWin = true;
        int columnNum = justPlacePiecesIndex % 3;	// 落子在第几列
        for (int i = columnNum; i <= columnNum + 6; i += 3) {
            if (allBattleCellInfo.get(i) != justPlacePiecesUserSeq) {
                isWin = false;
                break;
            }
        }
        if (isWin == true) {
            return justPlacePiecesUserSeq;
        }
        // 如果该落子位置处于对角线，检测斜向是否连成
        // 检测斜线方向（/）
        isWin = true;
        if (justPlacePiecesIndex == 2 || justPlacePiecesIndex == 4 || justPlacePiecesIndex == 6) {
            for (int i = 2; i <= 6; i += 2) {
                if (allBattleCellInfo.get(i) != justPlacePiecesUserSeq) {
                    isWin = false;
                    break;
                }
            }
        } else {
            isWin = false;
        }
        if (isWin == true) {
            return justPlacePiecesUserSeq;
        }
        // 检测反斜线方向（\）
        isWin = true;
        if (justPlacePiecesIndex == 0 || justPlacePiecesIndex == 4 || justPlacePiecesIndex == 8) {
            for (int i = 0; i <= 8; i += 4) {
                if (allBattleCellInfo.get(i) != justPlacePiecesUserSeq) {
                    isWin = false;
                    break;
                }
            }
        } else {
            isWin = false;
        }
        if (isWin == true) {
            return justPlacePiecesUserSeq;
        }

        // 发现没有一方胜利，则根据棋盘是否已满，返回对战未完成或平局
        for (int oneCellInfo : allBattleCellInfo) {
            if (oneCellInfo == 0) {
                return -1;
            }
        }
        return 0;
    }

}
