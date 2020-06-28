package com.jacey.game.gm.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description: TODO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class CookieUtil {

    /**
     * 设置cookie
     * @param response
     * @param key        cookie 的key
     * @param value      cookie 的值
     * @param maxAge     cookie 的保存时间（单位：秒）
     */
    public static void set(HttpServletResponse response,
                           String key,
                           String value,
                           int maxAge) {
        Cookie cookie = new Cookie(key, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);

    }

    /**
     * 获取cookie
     * @param request
     * @param name
     * @return
     */
    public static Cookie get(HttpServletRequest request,
                             String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie: cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }

        return null;
    }

}
