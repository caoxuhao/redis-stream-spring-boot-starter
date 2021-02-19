package com.eric.middleware.mybatisRedisPlugin.interceptors;

import com.eric.middleware.mybatisRedisPlugin.annotation.RedisAdd;
import com.eric.middleware.mybatisRedisPlugin.annotation.RedisDelete;
import com.eric.middleware.mybatisRedisPlugin.bean.RedisDelPojo;
import com.eric.middleware.mybatisRedisPlugin.bean.RedisMybatisCache;
import com.eric.middleware.mybatisRedisPlugin.redis.RedisCtrl;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * mybatis删除redis的拦截器
 * @author: caoxuhao
 * @Date: 2021/2/9 17:01
 */
@Slf4j
@ConditionalOnClass(RedisClient.class)
@Intercepts(
    {
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
    }
)
public class MybatisRedisDelInterceptor implements Interceptor {
    
    @Autowired(required = false)
    private RedisCtrl redisCtrl;

    //@RedisDelete的标志位 长度超过一般的函数名长度就行
    private static final int REDIS_DELETE_FLAG = 256;

    //需要处理的方法的缓存（修改方法比较少初始值默认16）
    private static Map<String, RedisMybatisCache> functionCache = new ConcurrentHashMap<>();

    //无需处理的方法的缓存
    private static Set<String> ignore = Collections.synchronizedSet(new HashSet<>());;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];

        Object parameter = args[1];
        Executor executor = (Executor) invocation.getTarget();
        if(redisCtrl == null){
            //直接交给下个拦截器处理
            return executor.update(ms, parameter);
        }

        String fullName = ms.getId();

        if(ignore.contains(fullName)){
            //无需处理
            return executor.update(ms, parameter);
        }

        //先从本地缓存取
        Object redisKeyIndexValue;
        String redisKeyPrefixName;
        RedisMybatisCache redisMybatisCache = functionCache.get(fullName);

        if(redisMybatisCache == null){

            //解析@RedisDelete
            RedisDelPojo pojo = parseRedisDelAnnotation(ms);

            if(pojo != null && pojo.ignore){
                //该方法需要忽略
                ignore.add(fullName);
                return executor.update(ms, parameter);
            }

            //当前调用函数为deleteByPrimaryKey updateByPrimaryKey方法，或者@RedisDelete
            int index = shouldDeleteRedis(fullName, pojo);
            if(index < 0){
                //不需要拦截直接交给下一个执行器处理
                ignore.add(fullName);
                return executor.update(ms, parameter);
            }

            //取redisKey的编号值
            String redisKeyIndex = ((pojo == null || StringUtils.isEmpty(pojo.redisKeyIndex)) ? "id" : pojo.redisKeyIndex);
            redisKeyIndexValue = MybatisRedisAddInterceptor.getRedisKeyIndex(parameter, redisKeyIndex);
            if(redisKeyIndexValue == null){
                //mybatis入参中没有这个变量对应的属性值，无法使用redis删除。
                //不需要拦截直接交给下一个执行器处理
                ignore.add(fullName);
                return executor.update(ms, parameter);
            }

            //获取redisKey前缀
            Class redisKeyPrefix = getRedisKeyPrefix(index, pojo, fullName);
            if(redisKeyPrefix == null){
                //没有redis前缀
                //不需要拦截直接交给下一个执行器处理
                ignore.add(fullName);
                return executor.update(ms, parameter);
            }

            redisKeyPrefixName = redisKeyPrefix.getSimpleName();

            //存本地缓存
            redisMybatisCache = new RedisMybatisCache();
            redisMybatisCache.setRedisKeyPrefixName(redisKeyPrefixName);
            redisMybatisCache.setRedisKeyIndex(redisKeyIndex);
            functionCache.put(fullName, redisMybatisCache);
        }else{
            //有本地缓存
            String redisKeyIndex = redisMybatisCache.getRedisKeyIndex();
            redisKeyIndexValue = MybatisRedisAddInterceptor.getRedisKeyIndex(parameter, redisKeyIndex);
            redisKeyPrefixName = redisMybatisCache.getRedisKeyPrefixName();
        }

        //删redis和数据库
        int update = doDelete(redisKeyPrefixName, redisKeyIndexValue, executor, ms, parameter);
        return update;
    }

    //删redis和数据库
    private int doDelete(String redisKeyPrefixName, Object redisKeyIndexValue,
                         Executor executor,MappedStatement ms, Object parameter) throws SQLException {
        //删redis
        boolean res = delete(redisKeyPrefixName, redisKeyIndexValue);
        if(!res){
            //redis删除失败，报错
            throw new RuntimeException("redis save error" + redisKeyPrefixName + ":" + redisKeyIndexValue);
        }
        //删数据库
        int update = executor.update(ms, parameter);
        if(update > 0){
            //删redis
            res = delete(redisKeyPrefixName, redisKeyIndexValue);
            if(!res){
                throw new RuntimeException("redis save error" + redisKeyPrefixName + ":" + redisKeyIndexValue);
            }
        }

        return update;
    }

    /**
     * 获取redisKey前缀
     */
    private Class getRedisKeyPrefix(int index, RedisDelPojo pojo, String fullName) throws ClassNotFoundException {

        Class redisKeyPrefix = null;
        String redisAddMethod = null;

        //取@RedisDelete的属性值
        if(index == REDIS_DELETE_FLAG){
            if(pojo.redisKeyPrefix != null){
                redisKeyPrefix = pojo.redisKeyPrefix;
            }

            if(!StringUtils.isEmpty(pojo.redisAddMethod)){
                redisAddMethod = pojo.redisAddMethod;
            }
        }

        Map<String, Class<?>> methodReturnMap = new HashMap();

        //没有自定义前缀，需要根据函数的返回值取
        if(redisKeyPrefix == null){
            //默认取selectByPrimaryKey的返回值
            if(StringUtils.isEmpty(redisAddMethod)){
                redisAddMethod = "selectByPrimaryKey";
            }

            String clzName = fullName.substring(0, index - 1);
            Class<?> clz = Class.forName(clzName);
            Method[] methods = clz.getMethods();
            for(Method method: methods){
                // 该Mapper里 @RedisDelete的redisAddMethod指定的方法。
                // 没有指定，则找selectByPrimaryKey方法
                // 没有selectByPrimaryKey则找@RedisAdd的方法
                if(method.getName().equals(redisAddMethod)){
                    methodReturnMap.put(redisAddMethod, method.getReturnType());
                    break;
                }

                RedisAdd redisAddAnnotation = method.getAnnotation(RedisAdd.class);
                if(redisAddAnnotation != null){
                    Class<?>[] redisAddReturnType = redisAddAnnotation.redisKeyPrefix();
                    if(redisAddReturnType != null && redisAddReturnType.length > 0){
                        methodReturnMap.put(method.getName(), redisAddReturnType[0]);
                    }else{
                        methodReturnMap.put(method.getName(), method.getReturnType());
                    }
                }
            }

            if(methodReturnMap.size() == 0){
                //没有@RedisAdd也没有selectByPrimaryKey方法，则无法自动确定redis前缀。不走redis逻辑
                return null;
            }

            //取用户指定，或默认的返回类型做前缀
            redisKeyPrefix = methodReturnMap.get(redisAddMethod);
            if (redisKeyPrefix == null){
                for (Class<?> returnType: methodReturnMap.values()) {
                    //取第一个@RedisAdd
                    redisKeyPrefix = returnType;
                    break;
                }
            }
        }

        return redisKeyPrefix;
    }


    /**
     * 判断是否需要删除redis
     * @param fullName
     * @param pojo
     * @return
     */
    private int shouldDeleteRedis(String fullName, RedisDelPojo pojo){

        //有@RedisDelete注解的
        if(pojo != null)
            return REDIS_DELETE_FLAG;

        //或者指定名称的sql
        int index = fullName.indexOf("deleteByPrimaryKey");
        if(index < 0)
            index = fullName.indexOf("updateByPrimaryKey");

        return index;
    }

    /**
     * redis删除
     * @param pojoClzName
     * @param id
     * @return
     */
    private boolean delete(String pojoClzName, Object id){
        String redisKey = new StringBuilder(pojoClzName).append(":").append(id.toString()).toString();
        return redisCtrl.deleteRedis(redisKey);
    }

    /**
     * 解析 @RedisDelete
     * @param ms
     * @return
     * @throws ClassNotFoundException
     */
    private RedisDelPojo parseRedisDelAnnotation(MappedStatement ms) throws ClassNotFoundException {

        RedisDelPojo pojo = null;

        String fullName = ms.getId();
        int index = fullName.lastIndexOf(".");
        String clzName = fullName.substring(0, index);
        String methodName = fullName.substring(index + 1);
        Class<?> clz = Class.forName(clzName);
        Method[] methods = clz.getMethods();
        for(Method method: methods) {
            //该Mapper里有selectByPrimaryKey方法
            if (method.getName().equals(methodName)) {
                RedisDelete annotation = method.getAnnotation(RedisDelete.class);
                if(annotation != null){
                    pojo = new RedisDelPojo();
                    pojo.ignore =  annotation.ignore();

                    String redisKeyIndex = annotation.redisKeyIndex();
                    if(!StringUtils.isEmpty(redisKeyIndex)){
                        pojo.redisKeyIndex = redisKeyIndex;
                    }

                    Class<?>[] classes = annotation.redisKeyPrefix();
                    if(classes != null && classes.length > 0){
                        pojo.redisKeyPrefix = classes[0];
                    }

                    String redisAddMethod = annotation.redisAddMethod();
                    if(!StringUtils.isEmpty(redisAddMethod)){
                        pojo.redisAddMethod = redisAddMethod;
                    }

                    break;
                }
            }
        }

        return pojo;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
