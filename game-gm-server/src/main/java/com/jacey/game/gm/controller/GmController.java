package com.jacey.game.gm.controller;

import com.jacey.game.common.constants.CookieConstant;
import com.jacey.game.db.entity.GmUserEntity;
import com.jacey.game.db.service.GmUserService;
import com.jacey.game.gm.enums.ResultEnum;
import com.jacey.game.gm.utils.CookieUtil;
import com.jacey.game.gm.utils.ResultVOUtil;
import com.jacey.game.gm.vo.ResultVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @Description: 服务器管理相关controller
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@RestController
@RequestMapping("/gm")
public class GmController {

    @Autowired
    private GmUserService gmUserService;

    /**
     * GM账户登录
     * @param gmUserName
     * @param passwordMD5
     */
    @GetMapping("/gmUserLogin")
    public ResultVO GmUserLogin(@RequestParam("gmUserName") String gmUserName, @RequestParam("passwordMD5") String passwordMD5,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 判断是否已登录
        Cookie cookie = CookieUtil.get(request, CookieConstant.TOKEN);
        // redis获取是否有token
        if (cookie != null &&
                !StringUtils.isEmpty(gmUserService.getGmUserTokenCache(cookie.getValue()))) {
            return ResultVOUtil.success();
        }
        // 没有，查询数据库
        GmUserEntity gmUserEntity = gmUserService.findGmUserByUsername(gmUserName);
        if (gmUserEntity == null) {
            return ResultVOUtil.error(ResultEnum.LOGIN_FAIL);
        }
        if (!passwordMD5.equalsIgnoreCase(gmUserEntity.getPasswordMD5())) {
            return ResultVOUtil.error(ResultEnum.LOGIN_FAIL);
        }
        // 生成登录token，并缓存
        String token = UUID.randomUUID().toString();
        Integer expire = CookieConstant.expire;        // 过期时间
        gmUserService.setGmUserTokenCache(token, expire);

        // 添加token到cookie中
        CookieUtil.set(response, CookieConstant.TOKEN, token, CookieConstant.expire);

        return ResultVOUtil.success();
    }


    @GetMapping("/executeGmCmd")
    public ResultVO ExecuteGmCmd() {
        // TODO
        return ResultVOUtil.success();
    }

}
