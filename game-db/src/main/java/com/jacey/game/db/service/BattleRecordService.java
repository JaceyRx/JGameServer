package com.jacey.game.db.service;


import com.jacey.game.db.entity.BattleRecordEntity;

/**
 * @Description: 对战数据归档处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface BattleRecordService {

    void saveBattleRecord(BattleRecordEntity battleRecordEntity);

}
