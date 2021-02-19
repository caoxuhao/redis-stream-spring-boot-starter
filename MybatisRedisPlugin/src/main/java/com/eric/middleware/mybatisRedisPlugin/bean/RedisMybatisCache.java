package com.eric.middleware.mybatisRedisPlugin.bean;

import lombok.Data;

/**
 * @author: caoxuhao
 * @Date: 2021/2/18 14:41
 */
@Data
public class RedisMybatisCache {
    private String redisKeyPrefixName;
    private String redisKeyIndex;
}
