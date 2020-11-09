package com.eric.middleware.streammq.spring.autoconfig;

import javax.annotation.PostConstruct;

import com.eric.middleware.streammq.spring.autoconfig.properties.StreamMQBean;
import com.eric.middleware.streammq.spring.config.StreamMQConfigManager;
import com.eric.middleware.streammq.spring.consumer.StreamMQConsumerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;


@Conditional(StreamMQCondition.class)
@Configuration
@EnableConfigurationProperties({StreamMQBean.class})
@ComponentScan(basePackages = {
	    		"com.eric.middleware.streammq.spring.config",
	    		"com.eric.middleware.streammq.spring.consumer",
	    		"com.eric.middleware.streammq.spring.redission",
				"com.eric.middleware.streammq.spring.core"
	    		})
public class StreamMQAutoConfiguration{
	
	@Autowired
	private StreamMQConfigManager streamMQConfigManager;
	
	@Autowired
	private StreamMQConsumerManager consumerManager;
	
	
	@PostConstruct
	public void init() {
		
		streamMQConfigManager.readConfig();
		
		//启动spring时候，自动启动所有接收的消费者
		consumerManager.createConsumers();
	}
}
