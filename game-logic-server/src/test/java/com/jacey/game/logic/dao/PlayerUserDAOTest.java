package com.jacey.game.logic.dao;

import com.jacey.game.db.entity.PlayUserEntity;
import com.jacey.game.db.repository.PlayUserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PlayerUserDAOTest {

    @Autowired
    private PlayUserRepository playUserDAO;

    @Test
    public void test1() {
        PlayUserEntity user = new PlayUserEntity();
        user.setUsername("张三");
        user.setNickname("老张");
        user.setPasswordMD5("xxxxxxxxxxxx");
        user.setRegistTimestamp(new Date());
        PlayUserEntity p =  playUserDAO.save(user);
        System.out.println(p.toString());
    }

    @Test
    public void test2() {
        PlayUserEntity user = playUserDAO.findOneByUsername("张三");
        System.out.println(user.toString());
    }

}