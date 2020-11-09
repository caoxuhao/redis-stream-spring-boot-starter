package com.eric.middleware.streammq.spring.consumer;

import com.eric.middleware.streammq.spring.core.StreamMQListener;
import lombok.Data;

@Data
public class ConsumerDefinitionBean {
	private String queueName;
	private StreamMQListener helper;
	private Class<?> listenerGenericsClass;//listener泛型类型
	private int retryTimes;
	private String consumerName;
	private String consumerGroup;
}
