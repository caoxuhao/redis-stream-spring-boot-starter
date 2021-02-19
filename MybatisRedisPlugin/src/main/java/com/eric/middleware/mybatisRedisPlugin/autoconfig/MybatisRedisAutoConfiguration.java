package com.eric.middleware.mybatisRedisPlugin.autoconfig;

import com.eric.middleware.mybatisRedisPlugin.interceptors.MybatisRedisAddInterceptor;
import com.eric.middleware.mybatisRedisPlugin.interceptors.MybatisRedisDelInterceptor;
import com.eric.middleware.mybatisRedisPlugin.redis.RedisCtrl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * mybatis redis 自动配置类
 * @author: caoxuhao
 * @Date: 2021/2/9 17:11
 */
@Configuration
@Conditional(MybatisRedisCondition.class)
@ConditionalOnClass({RedisCtrl.class, SqlSessionFactory.class})
@Import({MybatisRedisAddInterceptor.class, MybatisRedisDelInterceptor.class})
public class MybatisRedisAutoConfiguration {

}
