package com.jacey.game.db.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * @Description: 玩家状态
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Data
@Entity
@Table(name = "play_state")
public class PlayStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int userId;

    /** 在线状态（在线or离线）  */
    private int userOnlineState;

    /** 行为状态（none or 匹配中 or 对战中 等） */
    private int userActionState;

    /** 对战类型（1v1 or ..） */
    private int BattleType;

    /** 对战id */
    private String battleId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }
}
