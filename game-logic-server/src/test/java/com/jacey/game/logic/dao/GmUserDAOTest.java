package com.jacey.game.logic.dao;

import com.jacey.game.db.dao.GmUserDAO;
import com.jacey.game.db.entity.GmUserEntity;
import com.jacey.game.db.service.GmUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @Description: TODO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class GmUserDAOTest {

    @Autowired
    private GmUserDAO gmUserDAO;
    @Autowired
    private GmUserService gmUserService;

    @Test
    public void testSet() {
        gmUserDAO.setGmUserToken("aaaaaaa", 6000);
    }

    @Test
    public void testGet() {
        String gm = gmUserDAO.getGmUserToken("aaaaaaa");
        System.out.println(gm);
    }

    @Test
    public void findByName() {
        GmUserEntity gmUserEntity = gmUserService.findGmUserByUsername("admin");
        System.out.println(gmUserEntity.toString());
    }

}
