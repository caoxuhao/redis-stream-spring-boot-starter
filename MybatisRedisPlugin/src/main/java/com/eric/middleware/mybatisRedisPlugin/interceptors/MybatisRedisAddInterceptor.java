package com.eric.middleware.mybatisRedisPlugin.interceptors;

import com.eric.middleware.mybatisRedisPlugin.annotation.RedisAdd;
import com.eric.middleware.mybatisRedisPlugin.bean.RedisAddPojo;
import com.eric.middleware.mybatisRedisPlugin.bean.RedisMybatisCache;
import com.eric.middleware.mybatisRedisPlugin.redis.RedisCtrl;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * mybatis 插入redis的拦截器
 * @author: caoxuhao
 * @Date: 2021/2/9 17:01
 */
@Slf4j
@ConditionalOnClass(RedisClient.class)
@Intercepts(
    {
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
    }
)
public class MybatisRedisAddInterceptor implements Interceptor {
    
    @Autowired(required = false)
    private RedisCtrl redisCtrl;

    //需要处理的方法的缓存
    private static Map<String, RedisMybatisCache> functionCache = new ConcurrentHashMap<>();

    //无需处理的方法的缓存(查询方法相对较多，初始值设置大一点，避免频繁扩容导致性能下降)
    private static Set<String> ignore = Collections.synchronizedSet(new HashSet<>(128));

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];

        Object parameter = args[1];
        RowBounds rowBounds = (RowBounds) args[2];
        ResultHandler resultHandler = (ResultHandler) args[3];
        Executor executor = (Executor) invocation.getTarget();

        CacheKey cacheKey;
        BoundSql boundSql;

        if (args.length == 4) {
            //4 个参数时
            boundSql = ms.getBoundSql(parameter);
            cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
        } else {
            //6 个参数时
            cacheKey = (CacheKey) args[4];
            boundSql = (BoundSql) args[5];
        }

        if(redisCtrl == null){
            return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
        }

        String fullName = ms.getId();
        if(ignore.contains(fullName)){
            //无需处理
            return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
        }

        String redisKeyPrefixName;
        Object redisKeyIndexValue;
        RedisMybatisCache redisMybatisCache = functionCache.get(fullName);
        if(redisMybatisCache == null){

            //解析@RedisAdd
            RedisAddPojo pojo = parseRedisAddAnnotation(ms);

            //有@RedisAdd 或函数名是selectByPrimaryKey的，都自动进行redis缓存处理
            if(pojo != null || fullName.endsWith("selectByPrimaryKey")){

                if(pojo != null && pojo.ignore){
                    //该方法需要忽略
                    ignore.add(fullName);
                    return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
                }

                String redisKeyIndex = ((pojo == null || StringUtils.isEmpty(pojo.redisKeyIndex)) ? "id" : pojo.redisKeyIndex);
                redisKeyIndexValue = getRedisKeyIndex(parameter, redisKeyIndex);
                if(redisKeyIndexValue == null){
                    //没有redisIndex则不存redis
                    ignore.add(fullName);
                    return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
                }

                redisKeyPrefixName = getRedisKeyPrefixName(pojo, ms);

                //添加缓存
                redisMybatisCache = new RedisMybatisCache();
                redisMybatisCache.setRedisKeyIndex(redisKeyIndex);
                redisMybatisCache.setRedisKeyPrefixName(redisKeyPrefixName);
                functionCache.put(fullName, redisMybatisCache);
            }else {
                //其他语句正常处理
                ignore.add(fullName);
                return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
            }
        }else {
            //有本地缓存
            redisKeyPrefixName = redisMybatisCache.getRedisKeyPrefixName();
            String redisKeyIndex = redisMybatisCache.getRedisKeyIndex();
            redisKeyIndexValue = getRedisKeyIndex(parameter, redisKeyIndex);
        }

        return loadPojo(redisKeyIndexValue, redisKeyPrefixName, ms, parameter, executor, rowBounds, resultHandler, cacheKey, boundSql);
    }

    private String getRedisKeyPrefixName(RedisAddPojo pojo, MappedStatement ms){

        Class redisKeyPrefix = ((pojo == null) ? null : pojo.redisKeyPrefix);
        String redisKeyPrefixName = null;

        if(redisKeyPrefix == null){
            //没有，则默认用返回值的类型
            ResultMap resultMap = ms.getResultMaps().get(0);
            if(resultMap != null){
                redisKeyPrefixName = resultMap.getType().getSimpleName();
            }
        }else{
            redisKeyPrefixName = redisKeyPrefix.getSimpleName();
        }

        return redisKeyPrefixName;
    }


    private RedisAddPojo parseRedisAddAnnotation(MappedStatement ms) throws ClassNotFoundException {

        RedisAddPojo pojo = null;

        String fullName = ms.getId();
        int index = fullName.lastIndexOf(".");
        String clzName = fullName.substring(0, index);
        String methodName = fullName.substring(index + 1, fullName.length());
        Class<?> clz = Class.forName(clzName);
        Method[] methods = clz.getMethods();
        for(Method method: methods) {
            if (method.getName().equals(methodName)) {
                RedisAdd annotation = method.getAnnotation(RedisAdd.class);
                if(annotation == null)
                    continue;

                pojo = new RedisAddPojo();
                pojo.ignore =  annotation.ignore();

                String redisKeyIndex = annotation.redisKeyIndex();
                if(!StringUtils.isEmpty(redisKeyIndex)){
                    pojo.redisKeyIndex = redisKeyIndex;
                }

                Class<?>[] classes = annotation.redisKeyPrefix();
                if(classes != null && classes.length > 0){
                    pojo.redisKeyPrefix = classes[0];
                }

                break;
            }
        }

        return pojo;
    }

    /**
     * 根据对应的属性名，获取RedisKeyIndex   默认id
     */
     protected static Object getRedisKeyIndex(Object parameter, String fieldName){

        Object id = null;
        if(parameter != null){
            //入参是java基础类和包装类
            if(parameter instanceof MapperMethod.ParamMap){
                MapperMethod.ParamMap map = (MapperMethod.ParamMap)parameter;
                if(map != null){
                    try{
                        id = map.get(fieldName);
                    } catch (Exception e) {
                        //没有对应的属性直接不存redis
                    }
                }
            }else{
                //入参是一个类
                Class<?> parameterClass = parameter.getClass();
                try {
                    Field field = parameterClass.getDeclaredField(fieldName);
                    if(field != null){
                        field.setAccessible(true);
                        id = field.get(parameter);
                    }
                }catch (Exception e){}
            }
        }

        return id;
    }

    /**
     * 从redis读，如果没有则从数据库读，读完后再存redis
     * redis Key是Pojo类名+id
     */
    private List loadPojo(Object redisKeyIndex, String redisKeyPrefixName,
                          MappedStatement ms, Object parameter,
                          Executor executor, RowBounds rowBounds,
                          ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException {

        List resultList;


        String key = new StringBuilder(redisKeyPrefixName).append(":").append(redisKeyIndex.toString()).toString();
        Object object;
        try {
            object = redisCtrl.getRBucket(key).get();
            if (object == null) {
                resultList = executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
                if(!CollectionUtils.isEmpty(resultList)){
                    object = resultList.get(0);
                    redisCtrl.setValueAsync(key, object, 90, TimeUnit.DAYS);
                }
            }else{
                resultList = new ArrayList<>();
                resultList.add(object);
            }
        } catch (Exception e) {
            StackTraceElement stackTrace = e.getStackTrace()[1];
            if (log == null) {
                System.out.println(stackTrace.getClassName() + "从redis中获取" + key + "时出现异常，入参key：" + key);
            } else {
                log.info("{} 从redis中获取时出现异常，入参key：{}", stackTrace.getClassName(), key);
            }

            e.printStackTrace();

            resultList = executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
        }

        return resultList;

    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
