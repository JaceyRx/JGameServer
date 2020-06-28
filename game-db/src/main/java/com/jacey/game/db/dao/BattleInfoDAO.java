package com.jacey.game.db.dao;

import com.jacey.game.common.proto3.BaseBattle;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.db.constants.BattleRedisKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@Repository(value = "battleInfoDAO")
public class BattleInfoDAO {

	@SuppressWarnings("rawtypes")
	@Autowired
    RedisTemplate jedisTemplate;

	@Resource(name = "stringTemplate")
	// 当前进行中的对战battleId集合（set类型，value:进行中的对战battleId）
	private SetOperations<String, String> battlePlayingBattleIdsOps;
	@Resource(name = "integerTemplate")
	// 一场战斗中按先后手顺利排列的userId（list类型，value:先后手顺利排列的userId）
	private ListOperations<String, Integer> battleUserIdsOps;
	@Resource(name = "byteTemplate")
	// 一场战斗中当前回合的信息（map类型，key:battleId, value:protobuf定义的CurrentTurnInfo）
	private HashOperations<String, String, byte[]> battleCurrentTurnInfoOps;
	@Resource(name = "integerTemplate")
	// 一场战斗中当前棋盘中棋子信息（list类型，value:按下标位置排列的对应格子的棋子情况）
	private ListOperations<String, Integer> battleCellInfoOps;
	@Resource(name = "byteTemplate")
	// 一场战斗中发生的所有事件（list类型，value:protobuf定义的EventMsg）
	private ListOperations<String, byte[]> battleEventListOps;
	@Resource(name = "integerTemplate")
	// 一场战斗中上一个已发生事件的eventNum（map类型，key:battleId, value:该战场中上一个已发生事件的eventNum）
	private HashOperations<String, String, Integer> battleLastEventNumOps;
	@Resource(name = "longTemplate")
	// 一场战斗开始的时间戳（map类型，key:battleId, value:开始的时间戳）
	private HashOperations<String, String, Long> battleStartTimestampOps;
	@Resource(name = "integerTemplate")
	// 一场战斗尚未准备开始游戏（为了照顾有些客户端可能载入资源过慢，在一定时限内，所有玩家都准备就绪后再正式开始对战）的玩家userId（set类型，value:尚未准备开始游戏的玩家userId）
	private SetOperations<String, Integer> battleNotReadyUserIdsOps;
	@Resource(name = "byteTemplate")
	// 某日的对战战报（list类型，value:protobuf定义的BattleRecordData）
	private ListOperations<String, byte[]> battleRecordListOps;

	/**
	 * 添加当前进行中的对战battleId集合
	 * <key：battlePlayingBattleIds:battleType，values： battleId>
	 * @param battleId
	 * @param battleType
	 */
	public void addPlayingBattleId(String battleId, CommonEnum.BattleTypeEnum battleType) {
		battlePlayingBattleIdsOps.add(BattleRedisKeyHelper.getBattlePlayingBattleIdsRedisKey(battleType), battleId);
	}

	public void removePlayingBattleId(String battleId, CommonEnum.BattleTypeEnum battleType) {
		battlePlayingBattleIdsOps.remove(BattleRedisKeyHelper.getBattlePlayingBattleIdsRedisKey(battleType), battleId);
	}

	/**
	 * 一场战斗中按先后手顺利排列的userId  <key: battleUserIds: battleId  values: List<Integer> userIds>
	 * @param battleId
	 * @param userIds
	 */
	public void initOneBattleUserIds(String battleId, List<Integer> userIds) {
		battleUserIdsOps.rightPushAll(BattleRedisKeyHelper.getBattleUserIdsRedisKey(battleId), userIds);
	}

	public List<Integer> getOneBattleUserIds(String battleId) {
		return battleUserIdsOps.range(BattleRedisKeyHelper.getBattleUserIdsRedisKey(battleId), 0, -1);
	}

	@SuppressWarnings("unchecked")
	public void cleanOneBattleUserIds(String battleId) {
		jedisTemplate.delete(BattleRedisKeyHelper.getBattleUserIdsRedisKey(battleId));
	}

	public void setBattleCurrentTurnInfo(String battleId, BaseBattle.CurrentTurnInfo info) {
		battleCurrentTurnInfoOps.put(BattleRedisKeyHelper.getBattleCurrentTurnInfoRedisKey(), battleId,
				info.toByteArray());
	}

	/**
	 *  获取当前回合的信息
	 * @param battleId
	 * @return
	 * @throws Exception
	 */
	public BaseBattle.CurrentTurnInfo getBattleCurrentTurnInfo(String battleId) throws Exception {
		byte[] byteArray = battleCurrentTurnInfoOps.get(BattleRedisKeyHelper.getBattleCurrentTurnInfoRedisKey(),
				battleId);
		return byteArray == null ? null : BaseBattle.CurrentTurnInfo.parseFrom(byteArray);
	}

	public void removeBattleCurrentTurnInfo(String battleId) {
		battleCurrentTurnInfoOps.delete(BattleRedisKeyHelper.getBattleCurrentTurnInfoRedisKey(), battleId);
	}

	/**
	 * 设置棋盘信息
	 * @param battleId	对战id
	 * @param index		棋盘旗子索引
	 * @param value		旗子参数（1&2）
	 */
	public void setOneBattleCellInfo(String battleId, int index, int value) {
		battleCellInfoOps.set(BattleRedisKeyHelper.getBattleCellInfoRedisKey(battleId), index, value);
	}

	/**
	 * 初始化一场战斗中当前棋盘信息 <key: battleCellInfo：battleId  values: List<Integer> allCellInfo>
	 * @param battleId
	 * @param allCellInfo
	 */
	public void initAllBattleCellInfo(String battleId, List<Integer> allCellInfo) {
		battleCellInfoOps.rightPushAll(BattleRedisKeyHelper.getBattleCellInfoRedisKey(battleId), allCellInfo);
	}

	public Integer getOneBattleCellInfo(String battleId, int index) {
		return battleCellInfoOps.index(BattleRedisKeyHelper.getBattleCellInfoRedisKey(battleId), index);
	}

	public List<Integer> getAllBattleCellInfo(String battleId) {
		return battleCellInfoOps.range(BattleRedisKeyHelper.getBattleCellInfoRedisKey(battleId), 0, -1);
	}

	@SuppressWarnings("unchecked")
	public void cleanAllBattleCellInfo(String battleId) {
		jedisTemplate.delete(BattleRedisKeyHelper.getBattleCellInfoRedisKey(battleId));
	}

	public void addOneBattleEvent(String battleId, BaseBattle.EventMsg eventMsg) {
		battleEventListOps.rightPush(BattleRedisKeyHelper.getBattleEventListRedisKey(battleId), eventMsg.toByteArray());
	}

	/**
	 * 增加1并返回。如果key不存在，则创建并设置为1然后返回
	 * @param battleId
	 * @return
	 */
	public int addAndGetNextAvailableEventNum(String battleId) {
		return battleLastEventNumOps.increment(BattleRedisKeyHelper.getBattleLastEventNumRedisKey(), battleId, 1)
				.intValue();
	}

	public int getLastEventNum(String battleId) {
		Integer result = battleLastEventNumOps.get(BattleRedisKeyHelper.getBattleLastEventNumRedisKey(), battleId);
		return result == null ? 0 : result;
	}

	public void removeLastEventNum(String battleId) {
		battleLastEventNumOps.delete(BattleRedisKeyHelper.getBattleLastEventNumRedisKey(), battleId);
	}

	/**
	 * 设置对战开始时间  <key: battleStartTimestamp, values: battleId, startTimestamp>
	 * @param battleId
	 * @param startTimestamp
	 */
	public void setOneBattleStartTimestamp(String battleId, long startTimestamp) {
		battleStartTimestampOps.put(BattleRedisKeyHelper.getBattleStartTimestampRedisKey(), battleId, startTimestamp);
	}

	public long getOneBattleStartTimestamp(String battleId) {
		return battleStartTimestampOps.get(BattleRedisKeyHelper.getBattleStartTimestampRedisKey(), battleId);
	}

	public void removeOneBattleStartTimestamp(String battleId) {
		battleStartTimestampOps.delete(BattleRedisKeyHelper.getBattleStartTimestampRedisKey(), battleId);
	}

	/**
	 * 一场战斗尚未准备开始游戏
	 * （为了照顾有些客户端可能载入资源过慢，在一定时限内，所有玩家都准备就绪后再正式开始对战）的玩家userId
	 * （set类型，value:尚未准备开始游戏的玩家userId）
	 * <key: battleNotReadyUserIds:battleId, values: Integer[] userIds>
	 * @param battleId
	 * @param userIds
	 */
	public void initOnebattleNotReadyUserIds(String battleId, Integer[] userIds) {
		battleNotReadyUserIdsOps.add(BattleRedisKeyHelper.getBattleNotReadyUserIdsRedisKey(battleId), userIds);
	}

	public Set<Integer> getOnebattleNotReadyUserIds(String battleId) {
		return battleNotReadyUserIdsOps.members(BattleRedisKeyHelper.getBattleNotReadyUserIdsRedisKey(battleId));
	}

	public void removeOnebattleNotReadyUserId(String battleId, int userId) {
		battleNotReadyUserIdsOps.remove(BattleRedisKeyHelper.getBattleNotReadyUserIdsRedisKey(battleId), userId);
	}

	@SuppressWarnings("unchecked")
	public void cleanOnebattleNotReadyUserIds(String battleId) {
		jedisTemplate.delete(BattleRedisKeyHelper.getBattleNotReadyUserIdsRedisKey(battleId));
	}

	public void addOneBattleRecord(CommonEnum.BattleTypeEnum battleType, long oneDayZeroClockTimestamp,
								   BaseBattle.BattleRecordData battleRecordData) {
		battleRecordListOps.rightPush(
				BattleRedisKeyHelper.getBattleRecordListRedisKey(battleType, oneDayZeroClockTimestamp),
				battleRecordData.toByteArray());
	}
}
