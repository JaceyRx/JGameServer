package com.jacey.game.logic.service;

import com.jacey.game.common.proto3.CommonEnum;

/**
 * @Description: 匹配业务处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface MatchService {

    /**
     * 执行匹配动作
     * @throws Exception
     */
    void doMatch() throws Exception;

    /**
     * 添加玩家Id到匹配队列中
     * @param userId
     * @param battleType
     * @return
     * @throws Exception
     */
    boolean addMatchPlayer(int userId, CommonEnum.BattleTypeEnum battleType) throws Exception;

    /**
     * 从匹配队列中移除某个对象
     * @param userId
     * @param battleType
     * @return
     * @throws Exception
     */
    boolean removeMatchPlayer(int userId, CommonEnum.BattleTypeEnum battleType) throws Exception;

    /**
     * 停止匹配
     * @throws Exception
     */
    void stopMatch() throws Exception;

}
