package com.jacey.game.battle.manager;

import com.jacey.game.common.manager.IManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationFactory;

/**
 * @Description: 配置文件加载管理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class ConfigManager implements IManager {

    private static ConfigManager configManager = new ConfigManager();

    private static ConfigurationFactory factory;
    private static Configuration config;

    public static String REMOTE_GM_AKKA_PATH;               // GM服务器Akka path
    public static int BATTLE_SERVER_ID;                    // 当前battle服务器id
    public static String BATTLE_SERVER_AKKA_PATH;         // battle服务器akka oath

    private ConfigManager() {}

    public static ConfigManager getInstance() {
        return configManager;
    }


    @Override
    public void init() {
        log.info("------------ start load config ------------");
        loadConfig();
        log.info("------------ finish load config ------------");
    }

    @Override
    public void shutdown() {

    }

    private void loadConfig() {
        factory = new ConfigurationFactory("propertyConfig.xml");
        try {
            config = factory.getConfiguration();
        } catch (ConfigurationException e) {
            log.error("【config初始化失败】, exception = ", e);
            System.exit(0);
        }

        REMOTE_GM_AKKA_PATH = config.getString("remote.gm.akka.path");
        BATTLE_SERVER_ID = config.getInt("battle.server.id");
        BATTLE_SERVER_AKKA_PATH = config.getString("battle.server.akka.path");
    }

}
