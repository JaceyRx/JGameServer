package com.jacey.game.db.service.impl;

import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.utils.DateTimeUtil;
import com.jacey.game.db.entity.PlayStateEntity;
import com.jacey.game.db.entity.PlayUserEntity;
import com.jacey.game.db.repository.PlayUserRepository;
import com.jacey.game.db.service.PlayStateService;
import com.jacey.game.db.service.PlayUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

/**
 * @Description: 用户数据处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Service
public class PlayUserServiceImpl implements PlayUserService {

    @Autowired
    private PlayUserRepository playUserRepository;

    @Autowired
    private PlayStateService playStateService;

//    @Autowired
//    private UserDAO userDAO;

    @Override
    public boolean hasUsername(String username) {
        if (playUserRepository.findOneByUsername(username) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasUserId(int userId) {
        Optional<PlayUserEntity> optional = playUserRepository.findById(userId);
        if (optional.get() != null) {
            return true;
        }
        return false;
    }

    @Override
    public PlayUserEntity findPlayUserByUsername(String username) {
        return playUserRepository.findOneByUsername(username);
    }

    @Override
    public void createNewUser(PlayUserEntity playUserEntity) {

        PlayUserEntity p = playUserRepository.save(playUserEntity);

        // 初始化UserState
        PlayStateEntity playStateEntity = new PlayStateEntity();
        playStateEntity.setUserId(p.getUserId());
        playStateEntity.setUserOnlineState(CommonEnum.UserOnlineStateEnum.Offline_VALUE);
        playStateEntity.setUserActionState(CommonEnum.UserActionStateEnum.ActionNone_VALUE);
        playStateService.create(playStateEntity);

    }

    @Override
    public Integer getUserIdByUsername(String username) {
        PlayUserEntity playUserEntity = playUserRepository.findOneByUsername(username);
        return playUserEntity.getUserId();
    }

    @Override
    public CommonMsg.UserData getUserDataByUserId(int userId) throws Exception {

        // 如果获取不到则获取mysql的数据
        Optional<PlayUserEntity> optional = playUserRepository.findById(userId);
        PlayUserEntity playUserEntity = optional.get();
        // 构造user data体
        CommonMsg.UserData.Builder userDataBuilder = CommonMsg.UserData.newBuilder();
        userDataBuilder.setUserId(userId);
        userDataBuilder.setUsername(playUserEntity.getUsername());
        userDataBuilder.setNickname(playUserEntity.getNickname());
        userDataBuilder.setPasswordMD5(playUserEntity.getPasswordMD5());
        long registTimestamp = DateTimeUtil.dateToTimestamp(playUserEntity.getRegistTimestamp());
        userDataBuilder.setRegistTimestamp(registTimestamp);
        userDataBuilder.setRegistIp(playUserEntity.getRegistIp());
        if (playUserEntity.getLastLoginTimestamp() != null) {
            long lastLoginTimestamp = DateTimeUtil.dateToTimestamp(playUserEntity.getLastLoginTimestamp());
            userDataBuilder.setLastLoginTimestamp(lastLoginTimestamp);
        }
        if (playUserEntity.getLastLoginIp() != null) {
            userDataBuilder.setLastLoginIp(playUserEntity.getLastLoginIp());
        }

        return userDataBuilder.build();
    }

    @Override
    public CommonMsg.UserBriefInfo getUserBriefInfoByUserId(int userId) {
        Optional<PlayUserEntity> optional = playUserRepository.findById(userId);
        PlayUserEntity playUserEntity = optional.get();
        if (playUserEntity == null) {
            return null;
        } else {
            CommonMsg.UserBriefInfo.Builder builder = CommonMsg.UserBriefInfo.newBuilder();
            builder.setUserId(userId);
            builder.setNickname(playUserEntity.getNickname());
            builder.setUserState(playStateService.getUserStateByUserId(userId));
            return builder.build();
        }
    }

    @Override
    public void update(CommonMsg.UserData userData) {
        // 更新玩家信息
        PlayUserEntity playUserEntity = new PlayUserEntity();
        playUserEntity.setUserId(userData.getUserId());           // 用户id
        playUserEntity.setUsername(userData.getUsername());       // 用户名
        playUserEntity.setNickname(userData.getNickname());       // 昵称
        playUserEntity.setPasswordMD5(userData.getPasswordMD5()); // MD5密码
        Date lastLoginDate = Date.from(DateTimeUtil.timestampToInstant(userData.getLastLoginTimestamp()));
        Date registTimestamp = Date.from(DateTimeUtil.timestampToInstant(userData.getRegistTimestamp()));
        playUserEntity.setRegistIp(userData.getRegistIp());       // 注册ip
        playUserEntity.setRegistTimestamp(registTimestamp);       // 注册时间
        playUserEntity.setLastLoginTimestamp(lastLoginDate);      // 最后登录时间
        playUserEntity.setLastLoginIp(userData.getLastLoginIp()); // 最后登录ip

        // 保存到mysql
        playUserRepository.save(playUserEntity);
    }

    @Override
    public CommonMsg.UserInfo getUserInfoByUserId(int userId) throws Exception {
        CommonMsg.UserData userData = getUserDataByUserId(userId);
        if (userData == null) {
            return null;
        } else {
            CommonMsg.UserInfo.Builder builder = CommonMsg.UserInfo.newBuilder();
            builder.setUserId(userData.getUserId());
            builder.setUsername(userData.getUsername());
            builder.setNickname(userData.getNickname());
            builder.setUserState(playStateService.getUserStateByUserId(userData.getUserId()));     // 用户状态
            return builder.build();
        }
    }


}
