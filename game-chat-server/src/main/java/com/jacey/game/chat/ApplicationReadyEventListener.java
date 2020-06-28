package com.jacey.game.chat;

import com.jacey.game.chat.manager.*;
import com.jacey.game.common.manager.CoreManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @Description: manager 统一初始化
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class ApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        ConfigurableApplicationContext context = applicationReadyEvent.getApplicationContext();
        // 设置Spring context上下文
        SpringManager.getInstance().setContext(context);
        // 核心管理器注册
        CoreManager.getInstance().registManager(ConfigManager.getInstance());
        CoreManager.getInstance().registManager(SpringManager.getInstance());
        CoreManager.getInstance().registManager(MessageManager.getInstance());
        CoreManager.getInstance().registManager(OnlineClientManager.getInstance());

        CoreManager.getInstance().init();      // 对所有manager进行初始化
    }
}
