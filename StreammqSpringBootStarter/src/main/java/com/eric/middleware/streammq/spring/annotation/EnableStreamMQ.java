package com.eric.middleware.streammq.spring.annotation;

import com.eric.middleware.streammq.spring.autoconfig.StreamMQAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 允许开启StreamMQ（默认允许，可以通过这个注解阻止开启）
 * @author caoxuhao
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({StreamMQAutoConfiguration.class})
public @interface EnableStreamMQ {
	
	public boolean value() default true;
}
