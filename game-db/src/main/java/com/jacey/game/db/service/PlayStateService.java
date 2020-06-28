package com.jacey.game.db.service;

import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.db.entity.PlayStateEntity;

/**
 * @Description: 玩家状态操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface PlayStateService {

    /**
     * 更新玩家在线状态
     * @param userId
     * @param isOnline     在线状态
     */
    void changeUserOnlineState(int userId, boolean isOnline);

    /**
     * 获取 protobuf UserState
     * @param userId
     * @return
     */
    CommonMsg.UserState getUserStateByUserId(int userId);

    /**
     * 用户状态状态
     * @param userId
     * @return
     */
    PlayStateEntity getPlayStateByUserId(int userId);

    /**
     * 创建用户状态
     * @param playStateEntity
     */
    void create(PlayStateEntity playStateEntity);

    /**
     * 修改用户状态
     * @param userId            用户id
     * @param userActionState   用户行为状态
     * @param battleType        对战类型
     * @param battleId          对战id（非匹配对战状态传null）
     */
    void changeUserActionState(int userId, int userActionState, int battleType, String battleId);


}
