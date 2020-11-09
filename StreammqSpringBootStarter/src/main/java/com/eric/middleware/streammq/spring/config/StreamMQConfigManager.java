package com.eric.middleware.streammq.spring.config;

import java.util.HashMap;
import java.util.Map;

import com.eric.middleware.streammq.spring.autoconfig.properties.*;
import com.eric.middleware.streammq.spring.redission.RedissionStreamHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;


/**
 * 配置文件处理类
 * @author caoxuhao
 */
@Configuration
public class StreamMQConfigManager {
	
	@Autowired
	private StreamMQBean mqConfig;
	
	/**key: queueName, value:queue config*/
	private Map<String, StreamMQQueuesConfigBean> queuesConfigMap = new HashMap<>();

	private int retryTimes = RedissionStreamHelper.CONSUMER_DEFAULT_RETRY_TIMES;
	private int maxSize = RedissionStreamHelper.QUEUE_DEFAULT_MAX_SIZE;

	public void readConfig() {
		
		//读取总体配置
		StreamMQConfigBean config = mqConfig.getConfig();
		if(config != null) {
			StreamMQConsumerBean consumer = config.getConsumer();
			if(consumer != null && consumer.getRetryTimes() > 0)
				retryTimes = consumer.getRetryTimes();
			
			StreamMQQueueBean queue = config.getQueue();
			if(queue != null && queue.getMaxSize() > 0)
				maxSize = queue.getMaxSize();
		}
		
		
		//读取队列配置
		Map<String, StreamMQQueuesConfigBean> queues = mqConfig.getQueues();
		
		if(queues != null) {
			for (String key : queues.keySet()) {
				StreamMQQueuesConfigBean value = queues.get(key);
				if(value != null) {
					queuesConfigMap.put(value.getName(), value);
				}
			}
		}
	}
	
	public int getRetryTimes() {
		return retryTimes;
	}
	
	public Map<String, StreamMQQueuesConfigBean> getQueuesConfigMap() {
		return queuesConfigMap;
	}

	public int getMaxSize() {
		return maxSize;
	}
	
}
