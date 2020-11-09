package com.eric.middleware.streammq.spring.consumer;

public class MqMessage<V> {
	private String key;//暂时没用
	private V value;
	
	public MqMessage() {}
	
	public MqMessage(String key, V value) {
		super();
		this.key = key;
		this.value = value;
	}
	public V getValue() {
		return value;
	}
	public void setValue(V value) {
		this.value = value;
	}
	public void setKey(String key) {
		this.key = key;
	}
}
