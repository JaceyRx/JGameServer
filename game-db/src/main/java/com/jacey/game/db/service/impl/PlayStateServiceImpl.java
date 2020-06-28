package com.jacey.game.db.service.impl;

import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonEnum.UserOnlineStateEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.db.entity.PlayStateEntity;
import com.jacey.game.db.repository.PlayStateRepository;
import com.jacey.game.db.service.PlayStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description: 用户状态修改service
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Service
public class PlayStateServiceImpl implements PlayStateService {

    @Autowired
    private PlayStateRepository playStateRepository;



    @Override
    public void changeUserOnlineState(int userId, boolean isOnline) {
        PlayStateEntity playStateEntity = playStateRepository.findOneByUserId(userId);
        playStateEntity.setUserOnlineState(isOnline ? UserOnlineStateEnum.Online_VALUE : UserOnlineStateEnum.Offline_VALUE);
        playStateRepository.save(playStateEntity);
    }

    @Override
    public CommonMsg.UserState getUserStateByUserId(int userId) {
        PlayStateEntity playStateEntity = playStateRepository.findOneByUserId(userId);
        CommonMsg.UserState.Builder userState = CommonMsg.UserState.newBuilder();
        userState.setOnlineStateValue(playStateEntity.getUserActionState());
        userState.setActionStateValue(playStateEntity.getUserActionState());
        if (playStateEntity.getUserActionState() == CommonEnum.UserActionStateEnum.Matching_VALUE) {
            // 处于匹配中
            userState.setBattleTypeValue(playStateEntity.getBattleType());
        } else if (playStateEntity.getUserActionState() == CommonEnum.UserActionStateEnum.Playing_VALUE) {
            // 处于对战中
            userState.setBattleTypeValue(playStateEntity.getBattleType());
            userState.setBattleId(playStateEntity.getBattleId());
        }
        return userState.build();
    }

    @Override
    public PlayStateEntity getPlayStateByUserId(int userId) {
        return playStateRepository.findOneByUserId(userId);
    }

    @Override
    public void create(PlayStateEntity playStateEntity) {
        playStateRepository.save(playStateEntity);
    }

    @Override
    public void changeUserActionState(int userId, int userActionState, int battleType, String battleId) {
        PlayStateEntity playStateEntity = playStateRepository.findOneByUserId(userId);
        playStateEntity.setUserActionState(userActionState);
        playStateEntity.setBattleType(battleType);
        playStateEntity.setBattleId(battleId);
        playStateRepository.save(playStateEntity);
    }

}
