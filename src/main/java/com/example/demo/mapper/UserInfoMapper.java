package com.example.demo.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.example.demo.model.UserInfo;

@Mapper
public interface UserInfoMapper {
    @Select("select * from user_info where delete_flag=0 and user_name=#{name}")
    UserInfo queryUserByName(String name);

    @Insert("insert into user_info (user_name, password_hash) values (#{userName}, #{passwordHash})")
    void saveUser(UserInfo user);
}
