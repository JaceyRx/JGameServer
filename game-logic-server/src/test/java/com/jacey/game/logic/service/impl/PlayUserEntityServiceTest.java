package com.jacey.game.logic.service.impl;


import com.jacey.game.db.entity.PlayUserEntity;
import com.jacey.game.db.service.PlayUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PlayUserEntityServiceTest {

    @Autowired
    private PlayUserService playUserService;

    @Test
    public void findPlayUserByUsername() {
        PlayUserEntity user = playUserService.findPlayUserByUsername("Jacey");
        System.out.println(user.toString());
    }

    @Test
    public void save() {
        PlayUserEntity user = new PlayUserEntity();
        user.setUserId(1234);
        user.setUsername("123");
        user.setNickname("小明");
        user.setPasswordMD5("xxxxxxxxxxxx");
        user.setRegistTimestamp(new Date());
    }
}
