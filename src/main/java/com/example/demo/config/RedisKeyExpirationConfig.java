package com.example.demo.config;

import com.example.demo.service.ChatContextService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 监听 Redis key 过期事件，用于在上下文缓存过期时持久化到数据库。
 * 注意：需要 Redis 服务器开启 notify-keyspace-events 以至少包含 Ex。
 */
@Configuration
public class RedisKeyExpirationConfig extends KeyExpirationEventMessageListener {

    private final ChatContextService chatContextService;
    private final String keyPrefix;

    public RedisKeyExpirationConfig(RedisMessageListenerContainer listenerContainer,
                                    ChatContextService chatContextService,
                                    @Value("${app.chat.redis.key-prefix:chat:ctx:}") String keyPrefix) {
        super(listenerContainer);
        this.chatContextService = chatContextService;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        if (expiredKey != null && expiredKey.startsWith(keyPrefix)) {
            try {
                String idStr = expiredKey.substring(keyPrefix.length());
                Integer userId = Integer.parseInt(idStr);
                chatContextService.persistIfPresent(userId);
            } catch (Exception ignore) {
            }
        }
    }

    @Bean
    public static RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
