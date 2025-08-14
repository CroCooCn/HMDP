package com.hmdp.config;

import com.hmdp.utils.RedisLoginInterceptor;
import com.hmdp.utils.RedisTokenFreshInterceptor;
import com.hmdp.utils.SessionLoginInterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer{
    
    @Autowired(required = false)
    private SessionLoginInterceptor sessionLoginInterceptor;

    @Autowired(required = false)
    // false:当Spring容器中没有找到对应类型的Bean时，不会抛出异常，属性会被注入为null。这样可以让该依赖变为可选。
    private RedisLoginInterceptor redisLoginInterceptor;
   
    @Autowired(required = false)
    private RedisTokenFreshInterceptor redisTokenFreshInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        HandlerInterceptor interceptor;
        if(sessionLoginInterceptor==null) {
            interceptor=redisLoginInterceptor;
        }else {
            interceptor=sessionLoginInterceptor;
        }
        registry.addInterceptor(interceptor)
        .excludePathPatterns(
            //要放行的接口
            "/user/code",
            "/user/login",
            "/blog/hot",
            "/shop/**",
            "/shop-type/**",
            "/upload/**",
            "/voucher/**"
        );

        //如果application.yaml中启用了redis，再开启RedisTokenFreshInterceptor
        if(redisTokenFreshInterceptor != null) {
            registry.addInterceptor(redisTokenFreshInterceptor)
            .addPathPatterns("/**");
        }
    }
}
