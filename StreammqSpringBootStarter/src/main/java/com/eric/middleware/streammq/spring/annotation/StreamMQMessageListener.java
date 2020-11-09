package com.eric.middleware.streammq.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface StreamMQMessageListener {

	public String queueName();
	public int retryTimes() default 0;
	
	/**TODO 暂未开发完*/
	public String consumerName() default "";
	
	/**TODO 暂未开发完*/
	public String consumerGroup() default "";
}
