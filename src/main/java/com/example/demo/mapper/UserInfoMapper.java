package com.example.demo.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.demo.model.UserInfo;

@Mapper
public interface UserInfoMapper {
    @Select("select * from user_info where delete_flag=0 and user_name=#{name}")
    UserInfo queryUserByName(@Param("name") String name);

    @Insert("insert into user_info (user_name, password_hash) values (#{userName}, #{passwordHash})")
    void saveUser(UserInfo user);

    // 获取用户的信息（用于读取当前 avatar）
    @Select("select * from user_info where id = #{id} and delete_flag=0")
    UserInfo queryUserById(@Param("id") Integer id);

    // 更新用户的头像
    @Update("update user_info set avatar = #{avatarFilename} where id = #{id}")
    void updateUserAvatar(@Param("id") Integer id, @Param("avatarFilename") String avatarFilename);

    // 更新用户的昵称
    @Update("update user_info set user_name = #{nickName} where id = #{id}")
    void updateUserNickName(@Param("id") Integer id, @Param("nickName") String nickName);

    // 更新用户的密码哈希
    @Update("update user_info set password_hash = #{passwordHash} where id = #{id}")
    void updateUserPassword(@Param("id") Integer id, @Param("passwordHash") String passwordHash);
}
