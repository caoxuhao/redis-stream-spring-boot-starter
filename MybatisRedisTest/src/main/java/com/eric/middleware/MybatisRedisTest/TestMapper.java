package com.eric.middleware.MybatisRedisTest;

import org.apache.ibatis.annotations.*;

@Mapper
public interface TestMapper {

    @Select("select * from z_test where id = #{id}")
    TestBean selectByPrimaryKey(@Param("id") Integer id);

    @Update("update z_test set age = #{age} where id = #{id}")
    int updateByPrimaryKeySelective(TestBean testBean);

    @Delete("delete from z_test where id = #{id}")
    int deleteByPrimaryKey(@Param("id") Integer id);
}