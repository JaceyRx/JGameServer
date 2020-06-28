package com.jacey.game.db.repository;


import com.jacey.game.db.entity.PlayStateEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PlayStateEntityRepositoryTest {

    @Autowired
    PlayStateRepository playStateRepository;

    @Test
    public void findOneByUserId() {
        PlayStateEntity state = playStateRepository.findOneByUserId(13);
        state.setUserOnlineState(1);
        state.setUserActionState(1);
        playStateRepository.save(state);
    }
}
