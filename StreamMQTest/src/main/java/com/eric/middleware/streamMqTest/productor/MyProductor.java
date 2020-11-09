package com.eric.middleware.streamMqTest.productor;

import com.eric.middleware.streamMqTest.bean.User;
import com.eric.middleware.streammq.spring.core.StreamMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class MyProductor {

    @Autowired
    private StreamMQTemplate streamMQTemplate;

    @Value("${stream-mq.queues.queue1.name}")
    private String queueName;

    @RequestMapping("sendAsync")
    public boolean sendAsync(){
        User u = new User();
        Integer i = new Random(1000).nextInt();
        u.setId(i++);
        u.setName("啦啦啦啦-"+ i);

        streamMQTemplate.sendAsync(queueName, u, (res)->{
            if(res)
                System.out.println("send success");
            else
                System.err.println("send fail");
        });

        return true;
    }

    @RequestMapping("send")
    public boolean send(){
        User u = new User();
        Integer i = new Random(1000).nextInt();
        u.setId(i++);
        u.setName("哈哈哈哈-"+ i);

        return streamMQTemplate.send(queueName, u);
    }
}
