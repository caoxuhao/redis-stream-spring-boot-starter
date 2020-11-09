# redis-stream-spring-boot-starter

## 1. 采用 redis stream 做消息队列。
## 2. 整合 redis stream消息队列 到 springboot中。
## 3. 使用方法详见StreamMQTest项目。
### 3.1 消费者：
```
    @StreamMQMessageListener(queueName = "${stream-mq.queues.queue1.name}")
    public class MyConsumer implements StreamMQListener<User> {

        @Override
        public void onMessage(MqMessage<User> message) throws Exception {
            System.out.println("receive user:" + message.getValue());
        }
    }
```
### 3.2 生产者：
```    
    @Autowired
    private StreamMQTemplate streamMQTemplate;
    
    streamMQTemplate.send(queueName, yourObject);
```
    
### 3.3 配置：
支持全局设置和单独设置某队列，同时配置，以单独设置的为准。
```    
    stream-mq:
      enable: false （队列启用开关，默认不写为开，false为关）
      config:
        consumer:
          retryTimes: 3 （全局重试次数）
        queue:
          maxSize: 10     （全局队列最大长度）
      queues:
        queue1:
          name: myTestQueue
          maxSize: 1      (单独设置某队列最大长度)
          retryTimes: 1   （单独设置某队列重试次数）
```   
添加以下配置可在服务器启动时候，不存在未ack的队列时，不打印错误log
```   
   logging:
     level:
       io:
         netty:
          channel:
            DefaultChannelPipeline: off
      org:
        redisson:
          client:
            handler:
              CommandDecoder: off       
```
  
