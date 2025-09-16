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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.ResponseBody;

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

    @PostMapping("/avatar")
    @ResponseBody
    public Result<UserInfo> uploadAvatar(@RequestPart(name = "avatar", required = true) MultipartFile avatar, HttpSession session, @RequestParam(name = "id", required = false) Integer idParam) {
        // 获取用户 id：优先使用 session 登录用户
        UserInfo user = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        Integer userId = null;
        if (user != null) userId = user.getId();
        if (userId == null) userId = idParam; // 回退到请求参数
        if (userId == null) return Result.fail("未提供用户 ID 且未登录");

        // 校验文件类型和大小
        String contentType = avatar.getContentType();
        long maxSize = 2L * 1024L * 1024L; // 2MB
        if (contentType == null || !(contentType.equalsIgnoreCase("image/png") || contentType.equalsIgnoreCase("image/jpeg"))) {
            return Result.fail("只允许上传 PNG 或 JPEG 格式的图片");
        }
        if (avatar.getSize() > maxSize) {
            return Result.fail("文件过大，最大允许 2MB");
        }

        try {
            userService.updateUserAvatar(userId, avatar);
            // reload user info and update session
            UserInfo updated = userService.queryUserById(userId);
            if (updated != null) session.setAttribute(Constants.SESSION_USER_KEY, updated);
            return Result.success(updated);
        } catch (Exception ex) {
            return Result.fail("头像上传失败: " + ex.getMessage());
        }
    }

    @PostMapping("/nickname")
    @ResponseBody
    public Result<UserInfo> updateNickname(@RequestParam(name = "nickName", required = true) String nickName, HttpSession session, @RequestParam(name = "id", required = false) Integer idParam) {
        UserInfo user = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        Integer userId = null;
        if (user != null) userId = user.getId();
        if (userId == null) userId = idParam;
        if (userId == null) return Result.fail("未提供用户 ID 且未登录");

        try {
            UserInfo updated = userService.updateUserNickName(userId, nickName);
            // 同步更新 session 中的用户信息
            session.setAttribute(Constants.SESSION_USER_KEY, updated);
            return Result.success(updated);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail("更新昵称失败: " + ex.getMessage());
        }
    }

    @PostMapping("/password")
    @ResponseBody
    public Result<UserInfo> updatePassword(@RequestParam(name = "oldPassword", required = true) String oldPassword,
                                           @RequestParam(name = "newPassword", required = true) String newPassword,
                                           HttpSession session,
                                           @RequestParam(name = "id", required = false) Integer idParam) {
        UserInfo user = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        Integer userId = null;
        if (user != null) userId = user.getId();
        if (userId == null) userId = idParam;
        if (userId == null) return Result.fail("未提供用户 ID 且未登录");

        try {
            // 在服务器端对明文密码进行哈希（与 register 保持一致）
            String oldHash = HashUtil.sha256(oldPassword);
            String newHash = HashUtil.sha256(newPassword);
            UserInfo updated = userService.updateUserPassword(userId, oldHash, newHash);
            // 同步会话
            session.setAttribute(Constants.SESSION_USER_KEY, updated);
            return Result.success(updated);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail("更新密码失败: " + ex.getMessage());
        }
    }
}
