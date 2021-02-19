package com.eric.middleware.mybatisRedisPlugin.autoconfig;

import com.eric.middleware.mybatisRedisPlugin.annotation.EnableMybatisRedis;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * 自动加载条件判断类
 * @author: caoxuhao
 * @Date: 2021/2/19 15:05
 */
public class MybatisRedisCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        String enable = context.getEnvironment().getProperty("mybatis.redis.enable");
        if("false".equals(enable))
            return false;

        Map<String, Object> beans = context.getBeanFactory().getBeansWithAnnotation(EnableMybatisRedis.class);
        for (String name : beans.keySet()) {
            Object bean = beans.get(name);
            EnableMybatisRedis annotation = bean.getClass().getAnnotation(EnableMybatisRedis.class);
            if(!annotation.value())
                return false;
        }

        return true;
    }
}
