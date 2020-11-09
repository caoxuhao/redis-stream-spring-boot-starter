package com.eric.middleware.streammq.spring.autoconfig.properties;

import lombok.Data;

@Data
public class StreamMQConfigBean {
	private StreamMQConsumerBean consumer;
	private StreamMQQueueBean queue;
	
}
