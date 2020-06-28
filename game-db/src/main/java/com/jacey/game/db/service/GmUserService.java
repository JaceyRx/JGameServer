package com.jacey.game.db.service;

import com.jacey.game.db.entity.GmUserEntity;

/**
 * @Description: Gm用户操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public interface GmUserService {

    /**
     * 设置gm user token 缓存
     * @param token
     * @param expire  有效时间（单位：s）
     */
    void setGmUserTokenCache(String token, Integer expire);

    /**
     * 获取缓存的token
     * @param token
     * @return
     */
    String getGmUserTokenCache(String token);

    /**
     * 根据用户名获取Gm账户信息
     * @param username
     * @return
     */
    GmUserEntity findGmUserByUsername(String username);

}
