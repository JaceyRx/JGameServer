package com.jacey.game.db.service.impl;

import com.jacey.game.db.entity.BattleRecordEntity;
import com.jacey.game.db.repository.BattleRecordRepository;
import com.jacey.game.db.service.BattleRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description: 对战数据归档处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
@Service
public class BattleRecordServiceImpl implements BattleRecordService {

    @Autowired
    private BattleRecordRepository battleRecordRepository;

    @Override
    public void saveBattleRecord(BattleRecordEntity battleRecordEntity) {
        battleRecordRepository.save(battleRecordEntity);
    }
}
