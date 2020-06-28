package com.jacey.game.db.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * @Description: TODO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Data
@Entity
@Table(name = "gm_user")
public class GmUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;
    /** 玩家名称 */
    private String username;

    /** MD5加密密码 */
    private String passwordMD5;

}
