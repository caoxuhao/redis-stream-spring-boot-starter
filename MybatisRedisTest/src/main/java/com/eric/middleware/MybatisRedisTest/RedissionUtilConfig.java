//package com.eric.middleware.MybatisRedisTest;
//
//import com.eric.middleware.mybatisRedisPlugin.redis.RedisCtrl;
//import org.redisson.api.RedissonClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//
///**
// * RedissionUtil 初始化
// * 项目中配置了redis.host，且有RedissionUtils.class就可以自动注入
// * @author caoxuhao
// */
//@Primary
//@Configuration
//@ConditionalOnClass({ RedisCtrl.class })
//@ConditionalOnProperty(name = "spring.redis.host")
//public class RedissionUtilConfig {
//
//    @Autowired
//    private RedissonClient redissonClient;
//
//    @Bean
//    public RedisCtrl getRedisCtrl() {
//        return new RedisCtrl(redissonClient);
//    }
//}
