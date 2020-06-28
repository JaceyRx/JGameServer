package com.jacey.game.db.service.impl;

import com.jacey.game.db.dao.GmUserDAO;
import com.jacey.game.db.entity.GmUserEntity;
import com.jacey.game.db.repository.GmUserRepository;
import com.jacey.game.db.service.GmUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Service
public class GmUserServiceImpl implements GmUserService {

    @Autowired
    private GmUserDAO gmUserDAO;
    @Autowired
    private GmUserRepository gmUserRepository;

    @Override
    public void setGmUserTokenCache(String token, Integer expire) {
        gmUserDAO.setGmUserToken(token, expire);
    }

    @Override
    public String getGmUserTokenCache(String token) {
        return gmUserDAO.getGmUserToken(token);
    }

    @Override
    public GmUserEntity findGmUserByUsername(String username) {
        return gmUserRepository.findOneByUsername(username);
    }
}
