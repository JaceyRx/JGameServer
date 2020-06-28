package com.jacey.game.logic.manager;

import com.jacey.game.common.manager.IManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

/**
 * @Description: 处理Spring相关内容
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class SpringManager implements IManager {

    private static SpringManager instance = new SpringManager();

    public static SpringManager getInstance() {
        return instance;
    }

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // 检查redis是否已连接
        try {
            JedisConnectionFactory factory = getBean(JedisConnectionFactory.class);
            RedisConnection connection = factory.getConnection();
            if ("PONG".equals(connection.ping()) == false) {
                log.error("redis connect fail");
                System.exit(0);
            }
        } catch (Exception e) {
            log.error("redis connect fail, error = ", e);
            System.exit(0);
        }

    }

    @Override
    public void shutdown() {
    }

    public Object getBean(String beanName) {
        return context.getBean(beanName);
    }

    public <T> T getBean(Class<T> object) {
        return context.getBean(object);
    }

    public void setContext(ConfigurableApplicationContext context) {
        this.context = context;
    }
}
