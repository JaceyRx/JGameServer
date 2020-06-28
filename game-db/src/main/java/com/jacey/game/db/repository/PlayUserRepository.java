package com.jacey.game.db.repository;

import com.jacey.game.db.entity.PlayUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @Description: Mysql mysql 玩家信息表操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface PlayUserRepository extends JpaRepository<PlayUserEntity, Integer> {

    PlayUserEntity findOneByUsername(String user);

}
