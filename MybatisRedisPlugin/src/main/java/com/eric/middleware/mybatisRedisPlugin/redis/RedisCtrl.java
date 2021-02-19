package com.eric.middleware.mybatisRedisPlugin.redis;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.util.concurrent.TimeUnit;

/**
 * @author: caoxuhao
 * @Date: 2021/2/19 16:11
 */
@ConditionalOnBean(RedissonClient.class)
public class RedisCtrl {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取字符串对象
     */
    public <V> RBucket<V> getRBucket(String objectName) {
        RBucket<V> bucket = redissonClient.getBucket(objectName, JsonJacksonCodec.INSTANCE);
        return bucket;
    }

    /**
     * 赋值
     */
    public <V> void setValue(String key, V value) {
        getRBucket(key).set(value);
    }

    /**
     * 赋值(异步)
     */
    public <V> void setValueAsync(String key, V value, long redisExpiredTime, TimeUnit timeUnit) {
        getRBucket(key).setAsync(value, redisExpiredTime, timeUnit);
    }

    /**
     * 删除
     * @param key
     */
    private boolean deleteRBucket(String key) {
        return getRBucket(key).delete();
    }

    /**
     * 只删redis数据，如果没有直接返回true
     * @param rediskey
     * @return	是否删除成功
     */
    public boolean deleteRedis(String rediskey) {
        Object value = getRBucket(rediskey).get();
        //没有缓存，直接返回成功
        if(value == null)
            return true;

        return deleteRBucket(rediskey);
    }
}
