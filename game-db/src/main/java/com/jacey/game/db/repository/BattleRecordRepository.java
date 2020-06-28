package com.jacey.game.db.repository;

import com.jacey.game.db.entity.BattleRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @Description: 数据归档Repository
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface BattleRecordRepository extends JpaRepository<BattleRecordEntity, Integer> {
}
