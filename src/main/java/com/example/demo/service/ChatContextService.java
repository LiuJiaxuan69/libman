package com.example.demo.service;

import com.example.demo.mapper.ChatHistoryMapper;
import com.example.demo.model.ChatHistory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatContextService {

    private final StringRedisTemplate redis;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ChatContextService.class);

    // 内存中保留最近一次写入内容，用于 Redis 过期事件回写数据库
    private final Map<Integer, String> lastSnapshot = new ConcurrentHashMap<>();

    @Value("${app.chat.context.max-chars:16000}")
    private int maxChars;

    @Value("${app.chat.redis.ttl-seconds:1800}")
    private int ttlSeconds;

    public ChatContextService(StringRedisTemplate redis, ChatHistoryMapper chatHistoryMapper) {
        this.redis = redis;
        this.chatHistoryMapper = chatHistoryMapper;
    }

    private String redisKey(Integer userId) { return "chat:ctx:" + userId; }

    public void appendMessage(Integer userId, String role, String content) {
        if (userId == null || role == null || content == null) return;
        String key = redisKey(userId);
        String ctxJson = redis.opsForValue().get(key);
        List<Map<String, Object>> messages;
        try {
            if (ctxJson == null) {
                // 尝试从数据库加载旧历史
                ChatHistory db = chatHistoryMapper.selectByUserId(userId);
                if (db != null) {
                    ctxJson = db.getContextJson();
                    log.info("[ChatCtx] load from DB for user {} ({} chars)", userId, ctxJson == null ? 0 : ctxJson.length());
                }
            }
            if (ctxJson != null && !ctxJson.isBlank()) {
                messages = mapper.readValue(ctxJson, new TypeReference<>() {});
            } else {
                messages = new ArrayList<>();
            }
        } catch (Exception e) {
            messages = new ArrayList<>();
        }

        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        msg.put("ts", System.currentTimeMillis());
        messages.add(msg);

        // 长度控制（按字符数裁剪）
        String newJson = toJson(messages);
        if (newJson.length() > maxChars) {
            // 从头部移除直到满足
            int start = 0;
            while (start < messages.size() && newJson.length() > maxChars) {
                messages.remove(0);
                newJson = toJson(messages);
            }
        }

        // 写回 Redis 并设置 TTL
        redis.opsForValue().set(key, newJson, Duration.ofSeconds(ttlSeconds));
        log.info("[ChatCtx] wrote Redis key={} user={} size={} ttl={}s (role={})", key, userId, newJson.length(), ttlSeconds, role);
        lastSnapshot.put(userId, newJson);
    }

    public String getContextJson(Integer userId) {
        String key = redisKey(userId);
        String v = redis.opsForValue().get(key);
        if (v != null) {
            log.info("[ChatCtx] read from Redis key={} user={} size={}", key, userId, v.length());
            return v;
        }
        ChatHistory db = chatHistoryMapper.selectByUserId(userId);
        if (db != null) {
            log.info("[ChatCtx] fallback read from DB for user {} size={}", userId, db.getContextJson() == null ? 0 : db.getContextJson().length());
        } else {
            log.info("[ChatCtx] no context found for user {}", userId);
        }
        return db == null ? null : db.getContextJson();
    }

    public void persistIfPresent(Integer userId) {
        String json = lastSnapshot.remove(userId);
        if (json == null || json.isBlank()) return;
        ChatHistory existing = chatHistoryMapper.selectByUserId(userId);
        if (existing == null) {
            ChatHistory h = new ChatHistory();
            h.setUserId(userId);
            h.setContextJson(json);
            h.setTotalChars(json.length());
            chatHistoryMapper.insert(h);
        } else {
            existing.setContextJson(json);
            existing.setTotalChars(json.length());
            chatHistoryMapper.updateByUserId(existing);
        }
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }
}
