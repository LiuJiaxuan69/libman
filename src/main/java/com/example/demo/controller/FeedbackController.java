package com.example.demo.controller;

import com.example.demo.common.Constants;
import com.example.demo.common.Result;
import com.example.demo.model.Feedback;
import com.example.demo.model.UserInfo;
import com.example.demo.service.FeedbackService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // 提交反馈（登录用户可携带 userId，未登录则匿名）
    @PostMapping("/submit")
    public Result<Feedback> submit(@RequestParam("content") String content,
                                   @RequestParam(value = "rating", required = false) Integer rating,
                                   HttpServletRequest request) {
        Integer userId = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserInfo user = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
            if (user != null) userId = user.getId();
        }
        try {
            Feedback fb = feedbackService.submit(userId, content, rating);
            return Result.success(fb);
        } catch (Exception ex) {
            return Result.fail(ex.getMessage());
        }
    }

    // 分页列出反馈
    @GetMapping("/list")
    public Result<List<Feedback>> list(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                       @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Result.success(feedbackService.list(offset, limit));
    }

    // 获取单条反馈详情
    @GetMapping("/{id}")
    public Result<Feedback> get(@PathVariable("id") Integer id) {
        Feedback fb = feedbackService.get(id);
        if (fb == null) return Result.fail("记录不存在");
        return Result.success(fb);
    }
}
