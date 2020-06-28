package com.jacey.game.db.service;

import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.db.entity.PlayUserEntity;

/**
 * @Description: 用户数据处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface PlayUserService {

    /** 用户名是否已注册 */
    boolean hasUsername(String username);

    /** 用户id是否已注册 */
    boolean hasUserId(int userId);

    /** 根据用户名查询 */
    PlayUserEntity findPlayUserByUsername(String username);

    /** 保存玩家信息 */
    void createNewUser(PlayUserEntity playUserEntity);

    /**
     * 根据用户名查询userId
     * @param username
     * @return
     */
    Integer getUserIdByUsername(String username);

    /**
     * 获取存储的玩家用户数据
     * @param userId
     * @return
     */
    CommonMsg.UserData getUserDataByUserId(int userId) throws Exception;

    /**
     * 获取玩家简历信息（其他人可看）
     * @param userId
     * @return
     */
    CommonMsg.UserBriefInfo getUserBriefInfoByUserId(int userId);

    /**
     * 更新玩家信息
     * @param userData
     */
    void update(CommonMsg.UserData userData);

    /**
     * 获取玩家信息（玩家自己可查看的信息）
     * @param userId
     * @return
     */
    CommonMsg.UserInfo getUserInfoByUserId(int userId) throws Exception;
}
