package com.hmdp.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWorker {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    public long nextId(String key) {
        // 基准时间戳 - 2010年1月1日 00:00:00
        final long BASE_TIMESTAMP = 1262304000L;
        //从低位到高位：前32位是数据记录，后32位是当前时间
        
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));;
        long data=stringRedisTemplate.opsForValue().increment("icr:"+key+":"+date);
        
        long curTime=LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)-BASE_TIMESTAMP;
        long id=data+(curTime<<32);
        return id;
    
    }
    
}
