package com.jacey.game.battle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@SpringBootApplication
@ComponentScan("com.jacey.game")
@EnableJpaRepositories("com.jacey.game.db.repository")
@EntityScan("com.jacey.game.db.entity")
public class BattleServerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BattleServerApplication.class);
        app.addListeners(new ApplicationReadyEventListener());
        app.run(args);
    }

}
