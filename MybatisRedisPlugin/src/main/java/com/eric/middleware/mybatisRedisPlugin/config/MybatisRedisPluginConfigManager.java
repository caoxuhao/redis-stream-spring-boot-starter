package com.eric.middleware.mybatisRedisPlugin.config;

import com.eric.middleware.mybatisRedisPlugin.bean.MybatisRedisProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: caoxuhao
 * @Date: 2021/2/19 16:42
 */
public class MybatisRedisPluginConfigManager implements BeanPostProcessor {

    @Autowired
    private MybatisRedisProperties mybatisRedisProperties;

    private static Set<String> defaultAddFunctions = new HashSet<>();
    private static Set<String> defaultDelFunctions = new HashSet<>();

    public static Set<String> getDefaultAddFunctions() {
        return defaultAddFunctions;
    }

    public static Set<String> getDefaultDelFunctions() {
        return defaultDelFunctions;
    }

    public MybatisRedisPluginConfigManager(){
        defaultAddFunctions.add("selectByPrimaryKey");

        defaultDelFunctions.add("deleteByPrimaryKey");
        defaultDelFunctions.add("deleteByPrimaryKeySelective");
        defaultDelFunctions.add("updateByPrimaryKey");
        defaultDelFunctions.add("updateByPrimaryKeySelective");
    }

    //属性填充后，初始化前
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        List<String> userDefaultAddFunctions = mybatisRedisProperties.getDefaultAddFunctions();
        if(!CollectionUtils.isEmpty(userDefaultAddFunctions)){
            defaultAddFunctions.clear();
            defaultAddFunctions.addAll(userDefaultAddFunctions);
        }

        List<String> userDefaultDelFunctions = mybatisRedisProperties.getDefaultDelFunctions();
        if(!CollectionUtils.isEmpty(userDefaultDelFunctions)){
            defaultDelFunctions.clear();
            defaultDelFunctions.addAll(userDefaultDelFunctions);
        }

        return bean;
    }

}
