package com.mmall.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * @author YaningLiu
 * @date 2018/9/6/ 17:31
 */
@Slf4j
public class PropertiesUtil {

    private static Properties props;

    public synchronized static void init(String propertiesFileName) {
        if (StringUtils.isEmpty(propertiesFileName)) {
            propertiesFileName = "mmall.properties";
        }
        try {
            ClassLoader classLoader = PropertiesUtil.class.getClassLoader();
            InputStream resourceAsStream = classLoader.getResourceAsStream(propertiesFileName);
            props.load(new InputStreamReader(resourceAsStream, "UTF-8"));
        } catch (IOException e) {
            log.error("配置文件读取异常", e);
        }
    }

    static {
        props = new Properties();
        init(null);
    }

    public static String getProperty(String key) {
        String value = props.getProperty(key.trim());
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    public static String getProperty(String key, String defaultValue) {
        String value = props.getProperty(key.trim());
        if (StringUtils.isBlank(value)) {
            value = defaultValue;
        }
        return value.trim();
    }

}
