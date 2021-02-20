package com.eric.middleware.MybatisRedisTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: caoxuhao
 * @Date: 2021/2/20 9:46
 */
@RestController
public class TestController {

    @Autowired
    private TestMapper testMapper;

    @RequestMapping("u")
    public Object u(Integer id , Integer age){
        TestBean testBean = new TestBean();
        testBean.setId(id);
        testBean.setAge(age);
        return testMapper.updateByPrimaryKeySelective(testBean);
    }

    @RequestMapping("s")
    public Object s(Integer id){
        return testMapper.selectByPrimaryKey(id);
    }

    @RequestMapping("d")
    public Object d(Integer id){
        return testMapper.deleteByPrimaryKey(id);
    }
}
