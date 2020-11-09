package com.eric.middleware.streammq.spring.core;

import java.util.function.Consumer;

import com.eric.middleware.streammq.spring.autoconfig.properties.StreamMQQueuesConfigBean;
import com.eric.middleware.streammq.spring.config.StreamMQConfigManager;
import com.eric.middleware.streammq.spring.redission.RedissionStreamHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StreamMQTemplate {
	
	@Autowired
	private StreamMQConfigManager streamMQConfigManager;
	
	@Autowired
	private RedissionStreamHelper helper;
	
	/**
	 * 发送消息(同步)
	 * @param queueName
	 * @param value
	 * @return
	 */
	public <V> boolean send(String queueName, V value) {
		
		StreamMQQueuesConfigBean config = streamMQConfigManager.getQueuesConfigMap().get(queueName);
		
		int maxSize = getMaxSize(config);
		
		return helper.add(queueName, "0", value, maxSize);
	}
	
	/**
	 * 发送消息(同步)
	 * @param queueName
	 * @param key	(此参数已被废弃，客串任意值)
	 * @param value
	 * @return
	 */
	@Deprecated
	public <V> boolean send(String queueName, String key, V value) {
		
		StreamMQQueuesConfigBean config = streamMQConfigManager.getQueuesConfigMap().get(queueName);
		
		int maxSize = getMaxSize(config);
		
		return helper.add(queueName, key, value, maxSize);
	}
	
	/**
	 * 发送消息(异步)
	 * @param queueName		队列名称
	 * @param value
	 * @param callBack		结果回调函数
	 */
	public <V> void sendAsync(String queueName, V value, Consumer<Boolean> callBack) {
		
		StreamMQQueuesConfigBean config = streamMQConfigManager.getQueuesConfigMap().get(queueName);
		
		int maxSize = getMaxSize(config);
		
		helper.addAsync(queueName, "0", value, maxSize, callBack);
	}
	
	/**
	 * 发送消息(异步)
	 * @param queueName		队列名称
	 * @param key			(此参数已被废弃，客串任意值)
	 * @param value
	 * @param callBack		结果回调函数
	 */
	@Deprecated
	public <V> void sendAsync(String queueName, String key, V value, Consumer<Boolean> callBack) {
		
		StreamMQQueuesConfigBean config = streamMQConfigManager.getQueuesConfigMap().get(queueName);
		
		int maxSize = getMaxSize(config);
		
		helper.addAsync(queueName, key, value, maxSize, callBack);
	}
	
	/**
	 * 删除消息队列
	 * @param queueName
	 * @return
	 */
	public boolean deleteQueue(String queueName) {
		return helper.deleteQueue(queueName);
	}
	
	private int getMaxSize(StreamMQQueuesConfigBean config) {
		return (config != null && config.getMaxSize() > 0) ? config.getMaxSize() : streamMQConfigManager.getMaxSize();
	}
}
