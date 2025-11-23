package com.example.demo.controller;

import com.example.demo.common.Constants;
import com.example.demo.service.ChatContextService;
import com.example.demo.common.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/context")
public class ChatContextController {

    private final ChatContextService chatContextService;

    public ChatContextController(ChatContextService chatContextService) {
        this.chatContextService = chatContextService;
    }

    public record AppendRequest(String role, String content) {}

    @PostMapping("/append")
    public Result<String> append(@RequestBody AppendRequest req, HttpSession session) {
        Object userObj = session.getAttribute(Constants.SESSION_USER_KEY);
        if (userObj == null) return Result.fail("未登录");
        Integer userId = ((com.example.demo.model.UserInfo) userObj).getId();
        chatContextService.appendMessage(userId, req.role(), req.content());
        return Result.success("ok");
    }

    @GetMapping("/get")
    public Result<String> get(HttpSession session) {
        Object userObj = session.getAttribute(Constants.SESSION_USER_KEY);
        if (userObj == null) return Result.fail("未登录");
        Integer userId = ((com.example.demo.model.UserInfo) userObj).getId();
        String json = chatContextService.getContextJson(userId);
        return Result.success(json == null ? "[]" : json);
    }

    @PostMapping("/persist")
    public Result<String> persist(HttpSession session) {
        Object userObj = session.getAttribute(Constants.SESSION_USER_KEY);
        if (userObj == null) return Result.fail("未登录");
        Integer userId = ((com.example.demo.model.UserInfo) userObj).getId();
        chatContextService.persistIfPresent(userId);
        return Result.success("ok");
    }
}
