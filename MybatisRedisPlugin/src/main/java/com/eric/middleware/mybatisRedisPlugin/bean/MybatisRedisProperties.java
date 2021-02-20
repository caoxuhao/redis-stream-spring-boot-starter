package com.eric.middleware.mybatisRedisPlugin.bean;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: caoxuhao
 * @Date: 2021/2/19 16:43
 */
@Data
@ConfigurationProperties(prefix = "mybatis.redis")
public class MybatisRedisProperties {
    private List<String> defaultAddFunctions;
    private List<String> defaultDelFunctions;
}
