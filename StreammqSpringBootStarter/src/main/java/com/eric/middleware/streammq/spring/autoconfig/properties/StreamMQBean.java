package com.eric.middleware.streammq.spring.autoconfig.properties;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "stream-mq")
public class StreamMQBean {
	private boolean enable;
	private StreamMQConfigBean config;
	private Map<String, StreamMQQueuesConfigBean> queues;
}
