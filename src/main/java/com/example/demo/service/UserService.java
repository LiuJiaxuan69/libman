package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.mapper.UserInfoMapper;
import com.example.demo.model.UserInfo;

@Service
public class UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    public UserInfo queryUserByName(String name) {
        return userInfoMapper.queryUserByName(name);
    }

    public void saveUser(UserInfo user) {
        userInfoMapper.saveUser(user);
    }
}
