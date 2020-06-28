package com.jacey.game.logic.repository;

import com.jacey.game.db.entity.PlayStateEntity;
import com.jacey.game.db.repository.PlayStateRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;



@RunWith(SpringRunner.class)
@SpringBootTest()
@ComponentScan("com.jacey.game")
public class PlayStateEntityRepositoryTest {

    @Autowired
    private PlayStateRepository playStateRepository;

    @Test
    public void test1() {
        PlayStateEntity state = new PlayStateEntity();
        state.setUserId(252);
        state.setUserOnlineState(1);
        state.setUserActionState(0);
        playStateRepository.save(state);
    }

    @Test
    public void test2() {
        PlayStateEntity state = playStateRepository.findOneByUserId(7);
        state.setUserOnlineState(0);
        state.setUserActionState(0);
        playStateRepository.save(state);
    }

}
