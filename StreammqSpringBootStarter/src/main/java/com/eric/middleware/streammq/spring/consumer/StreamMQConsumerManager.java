package com.eric.middleware.streammq.spring.consumer;

import com.eric.middleware.streammq.spring.annotation.StreamMQMessageListener;
import com.eric.middleware.streammq.spring.autoconfig.properties.StreamMQQueuesConfigBean;
import com.eric.middleware.streammq.spring.config.StreamMQConfigManager;
import com.eric.middleware.streammq.spring.core.StreamMQListener;
import com.eric.middleware.streammq.spring.redission.RedissionStreamHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消息队列的消费者管理类
 * 自动读取@MqMessageListener注解，实现了MQListener接口的消费者（优先级更高）
 * 自动读取yml配置的，实现了MQListener接口的消费者(已经废弃)
 * 在线程池中运行各消费者
 * @author caoxuhao
 */
@Configuration
public class StreamMQConsumerManager {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private RedissionStreamHelper redissionStreamHelper;

    @Autowired
    private StandardEnvironment environment;

    @Autowired
    private StreamMQConfigManager config;

    /**
     * 启动所有消费者
     */
    public void createConsumers() {

        //读取配置文件
        int retryTimes = config.getRetryTimes();
        Map<String, StreamMQQueuesConfigBean> configMap = config.getQueuesConfigMap();

        //扫描注解，添加消费者定义到map
        Map<String, ConsumerDefinitionBean> consumerMap = scanAnnotation(retryTimes, configMap);

        //创建线程池执行所有消费者
        int size = consumerMap.size();
        ExecutorService poll = Executors.newFixedThreadPool(size);
        for (String key : consumerMap.keySet()) {
            ConsumerDefinitionBean consumer = consumerMap.get(key);
            poll.execute(()->{
                redissionStreamHelper.blockConsumer(consumer);
            });
        }
    }

    /**获取有@MqMessageListener的注解的消费者*/
    private Map<String, ConsumerDefinitionBean> scanAnnotation(int retryTimes, Map<String, StreamMQQueuesConfigBean> configMap) {

        Map<String, ConsumerDefinitionBean> map = new HashMap<>();

        Map<String, Object> beansWithAnnotationMap = context.getBeansWithAnnotation(StreamMQMessageListener.class);
        for (String key : beansWithAnnotationMap.keySet()) {
            Object value = beansWithAnnotationMap.get(key);
            Class<? extends Object> clz = value.getClass();

            Type actualType = null;
            boolean notFindInterface = true;

            //获取这个bean的所有接口
            Type[] types = clz.getGenericInterfaces();
            if(types != null && types.length > 0) {
                for (Type type : types) {
                    ParameterizedType parameterizedType = (ParameterizedType)type;
                    //检查继承的接口中，是否有MQListener
                    if(parameterizedType.getRawType() == StreamMQListener.class) {
                        //获取泛型的类型
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        //解析普通泛型
                        actualType = actualTypeArguments[0];

                        //解析list泛型
                        if(actualType instanceof ParameterizedType) {
                            actualType = ((ParameterizedType)actualType).getActualTypeArguments()[0];
                        }
                        notFindInterface = false;
                        break;
                    }
                }
            }

            if(notFindInterface) {
                continue;
            }

            StreamMQMessageListener annotation = value.getClass().getAnnotation(StreamMQMessageListener.class);
            String queueName = environment.resolvePlaceholders(annotation.queueName());
            if(StringUtils.isEmpty(queueName)) {
                continue;
            }

            if(annotation.retryTimes() > 0) {
                retryTimes = annotation.retryTimes();
            }

            ConsumerDefinitionBean consumerDefinitionBean = new ConsumerDefinitionBean();
            consumerDefinitionBean.setHelper((StreamMQListener)value);
            consumerDefinitionBean.setQueueName(queueName);
            consumerDefinitionBean.setRetryTimes(retryTimes);
            consumerDefinitionBean.setListenerGenericsClass((Class<?>)actualType);
            map.put(queueName, consumerDefinitionBean);

        }

        return map;
    }
}
