package com.jacey.game.db.repository;

import com.jacey.game.db.entity.PlayStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @Description: Mysql 玩家状态操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface PlayStateRepository extends JpaRepository<PlayStateEntity, Integer> {

    PlayStateEntity findOneByUserId(int userId);

}
