package com.eric.middleware.streammq.spring.core;

import com.eric.middleware.streammq.spring.consumer.MqMessage;

/**
 * 阻塞循环消费队列给用户自定义逻辑的接口
 * @author caoxuhao
 */
@FunctionalInterface
public interface StreamMQListener<V> {
	/**
	 * @param message 一条mssage的key和value
	 */
	public void onMessage(MqMessage<V> message) throws Exception;
}