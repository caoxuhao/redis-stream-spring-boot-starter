package com.eric.middleware.streamMqTest.consumer;

import com.eric.middleware.streamMqTest.bean.User;
import com.eric.middleware.streammq.spring.annotation.StreamMQMessageListener;
import com.eric.middleware.streammq.spring.consumer.MqMessage;
import com.eric.middleware.streammq.spring.core.StreamMQListener;

@StreamMQMessageListener(queueName = "${stream-mq.queues.queue1.name}")
public class MyConsumer implements StreamMQListener<User> {

    @Override
    public void onMessage(MqMessage<User> message) throws Exception {
        System.out.println("receive user:" + message.getValue());
    }
}
