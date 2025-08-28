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
public class RedisTokenFreshInterceptor implements HandlerInterceptor{
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Resource
    private UserStorageStrategy userStorageStrategy;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //authorization不存在，可能未登录，返回：放行
        String token=request.getHeader("authorization");
        if(token==null) {
            return true;
        }
        UserDTO user=userStorageStrategy.getUser(token);
        
        //用户不存在，说明可能未登录，也返回：放行
        if(user==null) {
            return true;
        }
        //保存用户信息到threadlocal 
        UserDTO userDTO = (UserDTO)user;
        UserHolder.saveUser(userDTO);

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
