package com.hmdp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "app.cache")
@Component
@Data
public class CacheConfig {
    private String login,breakdown,lock; 
}
