package com.eric.middleware.streammq.spring.redission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.redisson.api.PendingEntry;
import org.redisson.api.PendingResult;
import org.redisson.api.RFuture;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.client.RedisException;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;


/**
 * 通过redis stream实现mq
 * @author caoxuhao
 */
@Configuration
public class RedissionStreamUtils {
	
	@Autowired
    private RedissonClient redissonClient;

    /********************************************************************************************************
     * redission stream
     * use redission stream for message queue
     * @author caoxuhao
     * 目前没有多组消费的需求，仅开放单组，单消费者
     ********************************************************************************************************/
    
    private final static String STREAM_GROUP_NAME = "_GROUP_1"; 
    private final static String STREAM_CONSUMER_NAME = "_CONSUMER_1";
    
    private static String getDefaultConsumerName(String queueName) {
    	return new StringBuilder(queueName).append(STREAM_CONSUMER_NAME).toString();
    }
    
    private static String getDefaultGroupName(String queueName) {
    	return new StringBuilder(queueName).append(STREAM_GROUP_NAME).toString();
    }
    
    /**
     * 创建消息队列
     * @param queueName
     * @return
     */
    public RStream<String, String> createStream(String queueName) {
    	return createStream(queueName, getDefaultGroupName(queueName));
    }
    
    /**
     * 删除整个消息队列
     * @param stream
     * @return
     */
    public <V> boolean deleteStream(RStream<String, String> stream) {
    	return stream.delete();
    }
    
    /**
     * 获取消息队列
     * @param queueName
     * @return
     */
    public <V> RStream<String, String> getStream(String queueName) {
    	RStream<String, String> stream = redissonClient.getStream(queueName, JsonJacksonCodec.INSTANCE);
    	return stream;
    }
    
    /**
     * 添加数据到消息队列(同步)
     * @param stream
     * @param key
     * @param value
     * @param trimLen
     * @return
     */
    protected <V> StreamMessageId add(RStream<String, String> stream, String key, V value, int trimLen) {
    	String realValue = JSON.toJSONString(value);
    	StreamMessageId id = stream.add(key, realValue, trimLen, false);
    	return id;
    }
    
    /**
     * 添加数据到消息队列(异步)
     * @param stream
     * @param key
     * @param value
     */
    protected <V> void addAsync(RStream<String, String> stream, String key, V value, int trimLen,
    		Consumer<? super StreamMessageId> action) {
    	String realValue = JSON.toJSONString(value);
    	RFuture<StreamMessageId> future = stream.addAsync(key, realValue, trimLen, false);
    	future.thenAccept(action);
    	return;
    }
    
    /**
     * 阻塞获取消息队列的1条数据（阻塞时间默认512天）
     * @param stream
     * @return
     */
    public <V> Map<StreamMessageId, Map<String, V>> readOneStreamBlock(RStream<String, String> stream, Class<?> listenerGenericsClass) {
    	String name = stream.getName();
    	Map<StreamMessageId, Map<String, V>> map = readStream(stream, getDefaultGroupName(name),
    			getDefaultConsumerName(name), 1, listenerGenericsClass);
    	return map;
    }
    
    /**
     * 读取所有已经分给组，但是没有ack的数据（如果不是代码问题，可能是宕机等造成的）
     * @param stream
     * @return
     */
    public <V> Map<StreamMessageId, Map<String, V>> readPendingStream(RStream<String, String> stream, Class<?> listenerGenericsClass){
    	
    	Map<StreamMessageId, Map<String, V>> map = new HashMap<>();
    	
    	String name = stream.getName();
    	String defaultGroupName = getDefaultGroupName(name);
    	String defaultConsumerName = getDefaultConsumerName(name);
    	
    	try {
    		PendingResult pendingResult = stream.listPending(defaultGroupName);
        	
        	Map<String, Long> pendingMap = pendingResult.getConsumerNames();
        	if(pendingMap != null && pendingMap.size() > 0) {
        		Long size = pendingMap.get(defaultConsumerName);
        		if(size != null && size > 0) {
        			//此消费者有pending列表
        			List<PendingEntry> list = stream.listPending(defaultGroupName, defaultConsumerName,pendingResult.getLowestId(),
        					pendingResult.getHighestId(), size.intValue());
        	    	if(CollectionUtils.isEmpty(list)) {
        	    		return map;
        	    	}
        	    	
        	    	List<StreamMessageId> idList = list.stream().map(PendingEntry::getId).collect(Collectors.toList());
        	    	StreamMessageId[] ids = new StreamMessageId[idList.size()];
        	    	ids = idList.toArray(ids);
        	    	map = readNoAckStream(stream, defaultGroupName, defaultConsumerName, listenerGenericsClass);
        		}
        	}
		} catch (Exception e) {
			
			String errMsg = e.getMessage();
			//没有未应答队列
			if(!errMsg.startsWith("Unexpected exception while processing command")) {
				throw e;
			}
		}
    	
    	return map;
    }
    
    /**
     * 获取消息队列的数据（只读取，未分发的消息）
     * @param stream
     * @return
     */
    public <V> Map<StreamMessageId, Map<String, V>> readStream(RStream<String, String> stream, Class<?> listenerGenericsClass) {
    	String name = stream.getName();
    	Map<StreamMessageId, Map<String, V>> readGroup = readStream(stream, getDefaultGroupName(name),
    			getDefaultConsumerName(name), null, listenerGenericsClass);
    	return readGroup;
    }
    
    /**
     * 应答消息队列
     * @param stream
     * @param ids
     * @return
     */
    public <V> boolean ackStream(RStream<String, V> stream, StreamMessageId... ids) {
    	return ackStream(stream, getDefaultGroupName(stream.getName()), ids);
    }
    
    private <V> Map<StreamMessageId, Map<String, V>> readNoAckStream(RStream<String, String> stream, String groupName, String consumerName,
    		Class<?> listenerGenericsClass){
    	Map<StreamMessageId, Map<String, String>> map = new HashMap<> ();
    	try {
    		map = stream.readGroup(groupName, consumerName, new StreamMessageId(0));//拿epl里的
		} catch (RedisException e) {
			String errMsg = e.getMessage();
			
			if(errMsg.startsWith("NOGROUP No such key")) {
				//没有对应的group
				createGorup(stream, groupName);
			} else if(errMsg.startsWith("Unexpected exception while processing command")) {
				//不存在已经处理，但是没有ack的数据, 无需处理
			} else {
				throw e;
			}
			
		}
    	
    	return getFinalMap(map, listenerGenericsClass);
    }
    
    
    private <V> RStream<String, String> createStream(String queueName, String groupName) {
    	RStream<String, String> stream = getStream(queueName);
    	createGorup(stream, groupName);
    	return stream;
    }
    
    /**
     * 创建组
     * @param stream
     * @param groupName
     */
    private <V> void createGorup(RStream<String, String> stream, String groupName) {
    	try {
    		stream.createGroup(groupName, StreamMessageId.NEWEST);
		} catch (RedisException e) {
			String errMsg = e.getMessage();
			
			if(errMsg.startsWith("ERR The XGROUP subcommand requires the key to exist")) {
				//建组前，必须插入条数据。先加入数据，然后递归自己再次建组
				stream.add("0", "0");
				createGorup(stream, groupName);
				return;
			}
			
			//重复建group，不用管
			if(!errMsg.startsWith("BUSYGROUP Consumer Group name already exists")) {
				throw e;
			}
		}
    }
    
    /**
     * 只读取，未分发的消息
     * @param stream
     * @param groupName
     * @param consumerName
     * @param count
     * @return
     */
	private <V> Map<StreamMessageId, Map<String, V>> readStream(RStream<String, String> stream, String groupName,
    		String consumerName, Integer count, Class<?> listenerGenericsClass) {
    	
    	Map<StreamMessageId, Map<String, String>> map = new HashMap<> ();
    	
    	try {
    		
    		if(count == null)
        		map = stream.readGroup(groupName, consumerName);
        	else
        		map = stream.readGroup(groupName, consumerName, 512, TimeUnit.DAYS);
    		
    	} catch (RedisException e) {
			String errMsg = e.getMessage();
			
			//没有对应的group
			if(errMsg.startsWith("NOGROUP No such key")) {
				createGorup(stream, groupName);
			}else {
				throw e;
			}
		}
    	
    	return getFinalMap(map, listenerGenericsClass);
    }
    
    private <V> boolean ackStream(RStream<String, V> stream, String groupName, StreamMessageId... ids) {
    	long res = stream.ack(groupName, ids);
    	return (res == ids.length);
    }
    
    /**
     * redis取出结果 Map<StreamMessageId, Map<String, String>> 转 Map<StreamMessageId, Map<String, V>>
     * @param <V>
     * @param map
     * @param listenerGenericsClass
     * @return
     */
    @SuppressWarnings("unchecked")
	private <V> Map<StreamMessageId, Map<String, V>> getFinalMap(Map<StreamMessageId, Map<String, String>> map,
    		Class<?> listenerGenericsClass){
    	
    	Map<StreamMessageId, Map<String, V>> finalMap = new HashMap<> ();
    	for (StreamMessageId key: map.keySet()) {
    		
    		Map<String, V> mapV = new HashMap<>();
    		finalMap.put(key, mapV);
    		
    		Map<String, String> mapTmp = map.get(key);
    		for (String key2 : mapTmp.keySet()) {
    			String val = mapTmp.get(key2);
    			if(val.startsWith("{")) {
    				mapV.put(key2, (V)(JSON.parseObject(val, listenerGenericsClass)));
    			}else if(val.startsWith("[")) {
    				mapV.put(key2, (V)(JSON.parseArray(val, listenerGenericsClass)));
    			}
    		}
    	}
    	
    	return finalMap;
    }
}
