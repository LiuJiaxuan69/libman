package com.example.demo.controller;

import com.example.demo.ai.LibraryAiService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
@RequestMapping("/ai")
public class AiController {

    private final LibraryAiService aiService;

    public AiController(LibraryAiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body) {
        String sessionId = body == null ? null : String.valueOf(body.getOrDefault("sessionId", ""));
        String message = body == null ? null : String.valueOf(body.getOrDefault("message", ""));
        Map<String, Object> resp = new HashMap<>();
        if (message == null || message.isBlank()) {
            resp.put("status", "FAIL");
            resp.put("errorMessage", "message 不能为空");
            return resp;
        }
        String ensured = aiService.ensureSessionId(sessionId);
        String reply = aiService.chat(ensured, message);
        resp.put("status", "SUCCESS");
        resp.put("sessionId", ensured);
        resp.put("reply", reply);
        return resp;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Map<String, Object> body) {
        String sessionId = body == null ? null : String.valueOf(body.getOrDefault("sessionId", ""));
        String message = body == null ? null : String.valueOf(body.getOrDefault("message", ""));
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        if (message == null || message.isBlank()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("message 不能为空"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }
        aiService.simulatedStreamChat(sessionId, message,
                sid -> safeSend(emitter, SseEmitter.event().name("session").data(sid)),
                token -> safeSend(emitter, SseEmitter.event().name("token").data(token)),
                full -> {
                    safeSend(emitter, SseEmitter.event().name("done").data(full));
                    emitter.complete();
                },
                err -> {
                    safeSend(emitter, SseEmitter.event().name("error").data(err.getMessage() == null ? err.toString() : err.getMessage()));
                    emitter.complete();
                }
        );
        return emitter;
    }

    private void safeSend(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
