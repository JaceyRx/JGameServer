package com.jacey.game.battle.manager;

import com.jacey.game.common.manager.IManager;
import com.jacey.game.common.utils.ImportExcelUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 游戏系统配置数据加载
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class TableConfigManager implements IManager {

    private TableConfigManager(){}

    private static TableConfigManager instance = new TableConfigManager();

    public static TableConfigManager getInstance() {
        return instance;
    }

    // SystemConfig，系统配置表，key:paramKey, value:paramValue
    private Map<String, Object> systemConfigMap;

    public static final String TABLE_CONFIG_FILE_PATH = "tableConfig/";

    private ClassLoader classLoader = TableConfigManager.class.getClassLoader();

    @Override
    public void init() {
        log.info("------------ start load table config ------------");
        loadTableConfig();
        log.info("------------ finish load table config ------------");
    }

    private void loadTableConfig() {
        // SystemConfig，系统配置表，key:paramKey, value:paramValue
        systemConfigMap = new HashMap<String, Object>();
        InputStream inputStream = classLoader.getResourceAsStream(TABLE_CONFIG_FILE_PATH + "SystemConfig.xlsx");
        // 字段名映射
        Map<String, String> mappingMap = new HashMap<String, String>();
        mappingMap.put("参数名", "key");
        mappingMap.put("参数值", "value");
        try {
            systemConfigMap = ImportExcelUtil.parseExcel(inputStream, "SystemConfig.xlsx", mappingMap, 2);
        } catch (Exception e) {
            log.error("loadTableConfig error = ", e);
        }
        log.info("read table SystemConfig, data count = {}", systemConfigMap.size());
    }

    public void reloadTableConfig() {
        log.info("------------ reload table config ------------");
        loadTableConfig();
    }

    public Integer getSystemIntConfigByKey(String key) {
        if (systemConfigMap.containsKey(key)) {
            return Integer.parseInt((String) systemConfigMap.get(key));
        } else {
            return null;
        }
    }

    public String getSystemStringConfigByKey(String key) {
        return (String) systemConfigMap.get(key);
    }

    @Override
    public void shutdown() {

    }
}
