package com.eric.middleware.mybatisRedisPlugin.autoconfig;

import com.eric.middleware.mybatisRedisPlugin.bean.MybatisRedisProperties;
import com.eric.middleware.mybatisRedisPlugin.config.MybatisRedisPluginConfigManager;
import com.eric.middleware.mybatisRedisPlugin.interceptors.MybatisRedisAddInterceptor;
import com.eric.middleware.mybatisRedisPlugin.interceptors.MybatisRedisDelInterceptor;
import com.eric.middleware.mybatisRedisPlugin.redis.RedisCtrl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties({MybatisRedisProperties.class})
@Import({MybatisRedisPluginConfigManager.class,MybatisRedisAddInterceptor.class, MybatisRedisDelInterceptor.class})
public class MybatisRedisAutoConfiguration {

    public MybatisRedisAutoConfiguration(){
        System.out.println("插件启动了！！！！！！！！！！！！！！！！！！！！！！！！！！！！");
    }

}
