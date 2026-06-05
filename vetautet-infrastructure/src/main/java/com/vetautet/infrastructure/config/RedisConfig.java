package com.vetautet.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.sentinel.master:}")
    private String redisSentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private String redisSentinelNodes;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Cấu hình ObjectMapper để hỗ trợ Java 8 LocalDateTime
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // ĐĂNG KÝ MODULE XỬ LÝ DATE/TIME
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Lưu dạng chuỗi cho dễ đọc
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // Kích hoạt default typing để Jackson lưu kèm thông tin class vào JSON (mặc định dạng WRAPPER_ARRAY)
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance, 
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        return template;
    }

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        if (redisSentinelMaster != null && !redisSentinelMaster.isBlank()
                && redisSentinelNodes != null && !redisSentinelNodes.isBlank()) {
            String[] sentinelAddresses = java.util.Arrays.stream(redisSentinelNodes.split(","))
                    .map(String::trim)
                    .filter(node -> !node.isBlank())
                    .map(node -> node.startsWith("redis://") ? node : "redis://" + node)
                    .toArray(String[]::new);
            config.useSentinelServers()
                    .setMasterName(redisSentinelMaster)
                    .addSentinelAddress(sentinelAddresses);
        } else {
            config.useSingleServer()
                  .setAddress("redis://" + redisHost + ":" + redisPort);
        }
        return Redisson.create(config);
    }
}
