package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

import com.example.demo.common.Constants;
import com.example.demo.common.HashUtil;
import com.example.demo.model.UserInfo;
import com.example.demo.common.Result;

@RequestMapping("/user")
@RestController
public class UserController {
    @Autowired
    private UserService userService;

    @RequestMapping("/login")
    public Result<String> login(@RequestParam("userName") String userName, @RequestParam("password") String password, HttpSession session) {
        // 账号或密码为空
        if (!StringUtils.hasLength(userName) || !StringUtils.hasLength(password)) {
            return Result.fail("账号或密码不能为空");
        }
        UserInfo userInfo = userService.queryUserByName(userName);
        if (userInfo == null) {
            return Result.fail("用户不存在");
        }

        // 验证密码是否匹配
        String passwardHash = userInfo.getPasswordHash();
        if (passwardHash.equals(HashUtil.sha256(password))) {
            // 登录成功，将用户信息存入session
            session.setAttribute(Constants.SESSION_USER_KEY, userInfo);
            return Result.success("登录成功");
        }

        // 密码错误
        return Result.fail("密码错误");
    }

    @RequestMapping("/register")
    public Result<String> register(@RequestParam("userName") String userName, @RequestParam("password") String password) {

        // 账号或密码为空
        if (!StringUtils.hasLength(userName) || !StringUtils.hasLength(password)) {
            return Result.fail("账号或密码不能为空");
        }

        // 检查用户是否已存在
        UserInfo existingUser = userService.queryUserByName(userName);
        if (existingUser != null) {
            return Result.fail("用户已存在");
        }

        // 注册新用户
        UserInfo newUser = new UserInfo();
        newUser.setUserName(userName);
        newUser.setPasswordHash(HashUtil.sha256(password));
        userService.saveUser(newUser);

        return Result.success("注册成功");
    }

    @RequestMapping("/info")
    public Result<UserInfo> info(HttpSession session) {
        UserInfo user = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        if (user != null) {
            return Result.success(user);
        } else {
            return Result.fail("未登录");
        }
    }
}
