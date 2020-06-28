package com.jacey.game.db.constants;

import com.google.common.base.Joiner;
import com.jacey.game.common.proto3.CommonEnum;

/**
 * @Description: TODO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class GmRedisKeyHelper {

    private static final String SPRITE_STRING = ":";
    private static final Joiner joiner = Joiner.on(SPRITE_STRING);

    public static String join(Object... strs) {
        return joiner.join(strs);
    }

    public static String getGmUserTokeRedisKey(String tokenKey) {
        return join(GmRedisKeyConstant.GM_USER_TOKEN, tokenKey);
    }

}
