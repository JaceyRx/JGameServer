package com.jacey.game.gateway.manager;

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

    public static int CLIENT_PORT;
    /** 心跳配置 */
    public static int SOCKET_READER_IDLE_TIME;      // 空闲读
    public static int SOCKET_WRITER_IDLE_TIME;      // 空闲写
    public static int SOCKET_ALL_IDLE_TIME;         // 空闲（超过300.客户端无发包在，则为空闲）


    public static String REMOTE_GM_AKKA_PATH;       // GM服务器Akka papth
    public static int GATEWAY_ID;                   // 当前网关服务器id
    public static String GATEWAY_CONNECT_PATH;      // 网关服务器连接ip地址
    public static String GATEWAY_AKKA_PATH;         // 网关服务器akka oath

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
        CLIENT_PORT = config.getInt("client.port");

        SOCKET_READER_IDLE_TIME = config.getInt("socket.reader.idle.time");
        SOCKET_WRITER_IDLE_TIME = config.getInt("socket.writer.idle.time");
        SOCKET_ALL_IDLE_TIME = config.getInt("socket.all.idle.time");

        REMOTE_GM_AKKA_PATH = config.getString("remote.gm.akka.path");
        GATEWAY_ID = config.getInt("gateway.id");
        GATEWAY_CONNECT_PATH = config.getString("gateway.connect.path");
        GATEWAY_AKKA_PATH = config.getString("gateway.akka.path");
    }

}
