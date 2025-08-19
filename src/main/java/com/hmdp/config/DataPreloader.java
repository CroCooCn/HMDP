package com.hmdp.config;

import javax.annotation.Resource;

import com.hmdp.service.impl.ShopServiceImpl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DataPreloader implements CommandLineRunner{
    @Resource 
    private CacheConfig cacheConfig;

    @Resource
    private ShopServiceImpl shopService;
    
    @Override
    public void run(String...args) throws Exception {
        if("logical-expire".equals(cacheConfig.getCache())) {
            shopService.preloadShopsToRedis();
        }
    }
    
}
