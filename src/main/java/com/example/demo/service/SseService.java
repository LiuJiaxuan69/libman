package com.example.demo.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SseService {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        String id = UUID.randomUUID().toString();
        emitters.put(id, emitter);

        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        emitter.onError(ex -> emitters.remove(id));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            // ignore
        }
        return emitter;
    }

    public void sendEvent(String eventName, Object data) {
        String payload;
        try {
            payload = mapper.writeValueAsString(data);
        } catch (Exception e) {
            payload = String.valueOf(data);
        }
        for (Map.Entry<String, SseEmitter> e : emitters.entrySet()) {
            SseEmitter emitter = e.getValue();
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception ex) {
                emitters.remove(e.getKey());
            }
        }
    }
}
