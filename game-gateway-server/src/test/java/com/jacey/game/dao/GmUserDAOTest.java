package com.jacey.game.dao;

import com.jacey.game.GatewayServerApplicationTest;
import com.jacey.game.db.dao.GmUserDAO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description: TODO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class GmUserDAOTest {

    @Autowired
    private GmUserDAO gmUserDAO;

    @Test
    public void testSet() {
        gmUserDAO.setGmUserToken("abcd", 20);
    }

    @Test
    public void testGet() {

    }

}
