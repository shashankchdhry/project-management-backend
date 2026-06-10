package org.example.config;

import org.example.adapter.in.ws.BoardEventRelayListener;
import org.example.adapter.out.realtime.RealtimeEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    /**
     * Subscribes this instance to every project's board channel and forwards messages to local
     * STOMP sessions. The container retries in the background if Redis is unavailable, so it does
     * not block application startup.
     */
    @Bean
    public RedisMessageListenerContainer realtimeListenerContainer(RedisConnectionFactory connectionFactory,
                                                                   BoardEventRelayListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new PatternTopic(RealtimeEventPublisher.CHANNEL_PREFIX + "*"));
        return container;
    }
}
