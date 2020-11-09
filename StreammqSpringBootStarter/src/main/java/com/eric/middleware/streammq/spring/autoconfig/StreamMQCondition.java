package com.eric.middleware.streammq.spring.autoconfig;

import java.util.Map;

import com.eric.middleware.streammq.spring.annotation.EnableStreamMQ;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 任意一个EnableStreamMQ注解的值 = false 或者 mq.consumer.enable = false 都不会加载StreamMQ
 * @author caoxuhao
 *
 */
public class StreamMQCondition implements Condition{

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		
		Map<String, Object> beans = context.getBeanFactory().getBeansWithAnnotation(EnableStreamMQ.class);
		for (String name : beans.keySet()) {
			Object bean = beans.get(name);
			EnableStreamMQ annotation = bean.getClass().getAnnotation(EnableStreamMQ.class);
			if(!annotation.value())
				return false;
		}
		
		String enable = context.getEnvironment().getProperty("stream-mq.enable");
		if("false".equals(enable))
			return false;
		 
		return true;
	}
}
