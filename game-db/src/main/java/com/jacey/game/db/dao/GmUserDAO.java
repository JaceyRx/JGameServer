package com.jacey.game.db.dao;

import com.jacey.game.db.constants.GmRedisKeyConstant;
import com.jacey.game.db.constants.GmRedisKeyHelper;
import com.jacey.game.db.constants.RedisKeyHelper;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Description: gm服务器相关的redis操作
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Repository(value = "GmUserDAO")
public class GmUserDAO {

    @Resource(name = "stringTemplate")
    private ValueOperations<String, String> gmUserTokenOps;

    /**
     * 设置缓存token
     * @param token  存储的token
     * @param expire 超时时间（单位秒）
     */
    public void setGmUserToken(String token, Integer expire) {
        gmUserTokenOps.set(GmRedisKeyHelper.getGmUserTokeRedisKey(token),
                token,
                expire,
                TimeUnit.SECONDS);
    }

    /**
     * 获取缓存Token
     * @param token  存储的token
     */
    public String getGmUserToken(String token) {
        return gmUserTokenOps.get(GmRedisKeyHelper.getGmUserTokeRedisKey(token));
    }


}
