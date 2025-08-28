package com.hmdp.utils;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hmdp.dto.UserDTO;
import com.hmdp.service.UserStorageStrategy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
@ConditionalOnProperty(name="app.cache.login",havingValue = "redis")
public class RedisLoginInterceptor implements HandlerInterceptor{
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Resource
    private UserStorageStrategy userStorageStrategy;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token=request.getHeader("authorization");
        if(token==null) {
            response.setStatus(401);
            return false;
        }
        UserDTO user=userStorageStrategy.getUser(token);
        
        //用户不存在，进行拦截
        if(user==null) {
            response.setStatus(401);    //401-unauthorized
            return false;
        }
        //保存用户信息到threadlocal 
        UserHolder.saveUser(user);

        //刷新token的有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);

        return true;
    }


    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理ThreadLocal，避免内存泄漏
        UserHolder.removeUser();
    }
}
