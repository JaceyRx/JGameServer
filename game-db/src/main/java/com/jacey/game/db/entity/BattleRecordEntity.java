package com.jacey.game.db.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * @Description: 对战数据归档
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Data
@Entity
@Table(name = "battle_record")
public class BattleRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    /** 对战类型 */
    int battleType;

    /** 对战id */
    String battleId;

    /** 对战用户id String  逗号分隔 */
    String userIdList;

    /** 对战开始时间 */
    Date battleStartTimestamp;

    /** 对战结束时间 */
    Date battleEndTimestamp;

    /** 回合数 */
    int turnCount;

    /** 获胜方用户Id */
    int winnerUserId;

    /** 获胜原因 */
    int gameOverReason;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }
}
