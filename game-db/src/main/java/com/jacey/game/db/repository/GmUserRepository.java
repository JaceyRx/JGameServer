package com.jacey.game.db.repository;

import com.jacey.game.db.entity.BattleRecordEntity;
import com.jacey.game.db.entity.GmUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @Description: TODO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface GmUserRepository extends JpaRepository<GmUserEntity, Integer> {
    GmUserEntity findOneByUsername(String username);
}
