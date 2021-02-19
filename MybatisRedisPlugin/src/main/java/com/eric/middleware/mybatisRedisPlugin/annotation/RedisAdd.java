package com.eric.middleware.mybatisRedisPlugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: caoxuhao
 * @Date: 2021/2/10 10:14
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisAdd {
    /**
     * redis前缀，默认为selectByPrimaryKey方法的返回值
     */
    Class<?>[] redisKeyPrefix() default {};

    /**
     * redis key的编号所对应的变量名，默认为id
     */
    String redisKeyIndex() default "id";

    /**
     * 加在selectByPrimaryKey 方法上，若为true，可以不进行redis缓存操作
     */
    boolean ignore() default false;
}
