package com.eric.middleware.streammq.spring.autoconfig.properties;

import lombok.Data;

@Data
public class StreamMQQueuesConfigBean {
	private String name;
	private int maxSize;
}
