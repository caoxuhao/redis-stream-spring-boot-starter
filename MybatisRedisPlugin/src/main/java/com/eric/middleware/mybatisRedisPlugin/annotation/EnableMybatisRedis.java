package com.eric.middleware.mybatisRedisPlugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动配置注解（全局开关）
 * @author: caoxuhao
 * @Date: 2021/2/19 14:58
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnableMybatisRedis {
    /**
     * 默认开启mybatis 的 redis缓存功能。设置false可以关闭
     */
    boolean value() default true;
}
