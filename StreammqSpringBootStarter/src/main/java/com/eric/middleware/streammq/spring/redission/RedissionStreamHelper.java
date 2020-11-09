package com.eric.middleware.streammq.spring.redission;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.eric.middleware.streammq.spring.consumer.ConsumerDefinitionBean;
import com.eric.middleware.streammq.spring.consumer.MqMessage;
import com.eric.middleware.streammq.spring.core.StreamMQListener;
import org.redisson.api.RStream;
import org.redisson.api.StreamMessageId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



/**
 * RedissionStream操作类
 * @author caoxuhao
 */
@Component
public class RedissionStreamHelper {

	/**错误后重试次数*/
	public final static int CONSUMER_DEFAULT_RETRY_TIMES = 3;//正常执行1次，重试3次
	
	public final static int QUEUE_DEFAULT_MAX_SIZE = 50000;//队列最大长度
	
	@Autowired
	private RedissionStreamUtils redissionUtils;
	
	private Map<String, RStream<String, String>> streamMap = new HashMap<>();

	public boolean deleteQueue(String queueName) {
		RStream<String, String> stream = getQueue(queueName);
		if(stream != null)
			return redissionUtils.deleteStream(stream);
		return true;
	}
	
	/**
	 * 添加消息到消息队列(同步)
	 * @param queueName		队列名称
	 * @param key
	 * @param value
	 */
	public <V> boolean add(String queueName, String key, V value, int trimLen) {
		RStream<String, String> stream = getQueue(queueName);
		return (redissionUtils.add(stream, key, value, trimLen) != null);
	}
	/**
	 * 添加消息到消息队列
	 * @param queueName		队列名称
	 * @param key
	 * @param value
	 */
	public <V> void addAsync(String queueName, String key, V value, int trimLen, Consumer<Boolean> callBack) {
		RStream<String, String> stream = getQueue(queueName);
		redissionUtils.addAsync(stream, key, value, trimLen, (StreamMessageId)->{
			//System.out.println("消息已发送:StreamMessageId"+StreamMessageId);
			callBack.accept(StreamMessageId != null);
		});
	}
	
	/**
	 * 阻塞循环消费队列
	 */
	@SuppressWarnings("unchecked")
	public <V> void blockConsumer(ConsumerDefinitionBean consumerDefinitionBean) {
		
		String queueName = consumerDefinitionBean.getQueueName();
		StreamMQListener helper = consumerDefinitionBean.getHelper();
		Integer retryTimes = consumerDefinitionBean.getRetryTimes();
		Class<?> listenerGenericsClass = consumerDefinitionBean.getListenerGenericsClass();
		
		retryTimes++;
		
		RStream<String, String> stream = getQueue(queueName);
		//读已分发，未ack的，并处理
		Map<StreamMessageId, Map<String, V>> readStream = redissionUtils.readPendingStream(stream, listenerGenericsClass);
		doBlockConsumer(stream, helper, readStream, retryTimes);
		
		do {
			//读取新分发的数据，并处理
			Map<StreamMessageId, Map<String, V>> readStream2 = redissionUtils.readOneStreamBlock(stream, listenerGenericsClass);
			doBlockConsumer(stream, helper, readStream2, retryTimes);
		}while(true);
	}
	
	private <V> void doBlockConsumer(RStream<String, String> stream, StreamMQListener<V> helper,
			Map<StreamMessageId, Map<String, V>> readStream, int retryTimes) {
		for (StreamMessageId msgId : readStream.keySet()) {
			
			for (int i = 0; i < retryTimes; i++) {
				try {
					//由于insert时候每条消息只放一个键值对，所以这边只取第一条
					Map<String, V> map = readStream.get(msgId);
					if(map != null && map.size() > 0) {
						for (String key : map.keySet()) {
							V value = map.get(key);
							helper.onMessage(new MqMessage<V>(key, value));
							break;
						}
					}
					
					//应答
					redissionUtils.ackStream(stream, msgId);
					break;//任意一次成功，跳出重试循环
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 获取队列
	 * @param queueName
	 * @return
	 */
	private RStream<String, String> getQueue(String queueName) {
		
		RStream<String, String> stream = null;
		
		stream = streamMap.get(queueName);
		//有缓存不走同步方法，效率高
		if(stream == null) {
			synchronized (queueName.intern()) {
				//2次获取，被锁住的时候，可能这个key被赋值了
				stream = streamMap.get(queueName);
				if(stream == null) {
					stream = redissionUtils.createStream(queueName);
					streamMap.put(queueName, stream);
				}
			}
		}
		
		return stream;
	}
}
