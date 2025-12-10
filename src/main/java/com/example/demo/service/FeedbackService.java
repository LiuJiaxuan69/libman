package com.example.demo.service;

import com.example.demo.mapper.FeedbackMapper;
import com.example.demo.model.Feedback;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {
    private final FeedbackMapper feedbackMapper;

    public FeedbackService(FeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    public Feedback submit(Integer userId, String content, Integer rating) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("反馈内容不能为空");
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("评分范围为 1-5");
        }
        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setContent(content.trim());
        fb.setRating(rating);
        feedbackMapper.insert(fb);
        return feedbackMapper.findById(fb.getId());
    }

    public List<Feedback> list(int offset, int limit) {
        if (limit <= 0) limit = 20;
        if (offset < 0) offset = 0;
        return feedbackMapper.list(offset, limit);
    }

    public Feedback get(Integer id) {
        return feedbackMapper.findById(id);
    }
}
