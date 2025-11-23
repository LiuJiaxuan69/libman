package com.example.demo.ai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class LibraryAiService {

    private final LibraryAssistant assistant;

    public LibraryAiService(ChatLanguageModel chatLanguageModel, MapperTools mapperTools) {
        ChatMemoryProvider memoryProvider = sessionId -> MessageWindowChatMemory.withMaxMessages(20);
        this.assistant = AiServices.builder(LibraryAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(List.of(mapperTools))
                .chatMemoryProvider(memoryProvider)
                .build();
    }

    public String ensureSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }

    public String chat(String sessionId, String message) {
        String sid = ensureSessionId(sessionId);
        String full = assistant.chat(sid, message);
        full = cleanReply(full, sid);
        return full;
    }

    public void simulatedStreamChat(String sessionId, String message,
                                    java.util.function.Consumer<String> sessionEventConsumer,
                                    java.util.function.Consumer<String> tokenConsumer,
                                    java.util.function.Consumer<String> completeConsumer,
                                    java.util.function.Consumer<Throwable> errorConsumer) {
        String sid = ensureSessionId(sessionId);
        sessionEventConsumer.accept(sid);
        // 同步生成完整回复
        String generated = assistant.chat(sid, message);
        final String full = cleanReply(generated, sid);
        new Thread(() -> {
            try {
                // 按 Unicode code point 逐字输出，过滤器可自定义修改字符
                int[] cps = full.codePoints().toArray();
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < cps.length; i++) {
                    String ch = new String(Character.toChars(cps[i]));
                    ch = filterChar(ch, builder);
                    if (ch.isEmpty()) {
                        continue; // 跳过被过滤掉的字符
                    }
                    builder.append(ch);
                    tokenConsumer.accept(ch); // 单字发送
                    Thread.sleep(25); // 控制节奏，降低卡顿感
                }
                completeConsumer.accept(builder.toString());
            } catch (Throwable e) {
                errorConsumer.accept(e);
            }
        }, "ai-stream-" + sid).start();
    }

    /**
     * 简单字符过滤：
     * 1. 去除回车符 \r
     * 2. 合并连续空格为单个空格（不影响中文标点）
     * 3. 保留换行但避免出现超过2个连续换行
     */
    private String filterChar(String ch, StringBuilder current) {
        if ("\r".equals(ch)) return "";
        // 合并多余空格
        if (" ".equals(ch)) {
            int len = current.length();
            if (len > 0 && current.charAt(len - 1) == ' ') {
                return ""; // 跳过重复空格
            }
        }
        // 限制连续换行
        if ("\n".equals(ch)) {
            int len = current.length();
            int nlCount = 0;
            for (int i = len - 1; i >= 0 && i >= len - 4; i--) { // 最多检查最近4字符
                if (current.charAt(i) == '\n') nlCount++; else break;
            }
            if (nlCount >= 2) { // 已经有2个连续换行了
                return "";
            }
        }
        return ch;
    }

    // 如果回复以 sessionId 或任意 UUID 起始，剥离它以及后续的空白
    private String stripLeadingUuid(String text, String sid) {
        if (text == null) return "";
        String trimmed = text.trim();
        // 先匹配精确的当前 sid
        if (trimmed.startsWith(sid)) {
            return trimmed.substring(sid.length()).stripLeading();
        }
        // 再尝试任意 UUID 正则
        if (trimmed.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}.*")) {
            return trimmed.replaceFirst("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\s*", "");
        }
        return text;
    }

    // 进一步清洗：头部 UUID + 任何内嵌的 sid/UUID（避免模型回声）
    private String cleanReply(String text, String sid) {
        if (text == null) return "";
        String original = text;
        String noHead = stripLeadingUuid(original, sid);
        // 若模型回显 sid，全部移除
        if (sid != null && !sid.isBlank()) {
            noHead = noHead.replace(sid, "");
        }
        // 通用 UUID 模式移除（可能有多个）
        noHead = noHead.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "");
        // 有些模型可能把 UUID 拆开或追加中文字符立刻跟随，这里再用前缀宽松匹配截掉开头的 UUID 字符串集合
        noHead = noHead.replaceFirst("^[0-9a-fA-F-]{36,}\s*", "");
        String cleaned = noHead.strip();
        // Debug 日志（可根据需要删除）
        if (cleaned.length() != original.length()) {
            System.out.println("[AI CLEAN] before='" + original + "' after='" + cleaned + "'");
        }
        return cleaned;
    }
}
