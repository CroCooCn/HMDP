package com.hmdp.utils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hmdp.dto.UserDTO;
import com.hmdp.service.UserStorageStrategy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@ConditionalOnProperty(name="app.type",havingValue = "session")
public class SessionLoginInterceptor implements HandlerInterceptor{
    
    @Resource
    private UserStorageStrategy userStorageStrategy;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //session方式：从session中取出user
        HttpSession session=request.getSession();
        UserDTO user=userStorageStrategy.getUser(null);

        //用户不存在，进行拦截
        if(user==null) {
            response.setStatus(401);    //401-unauthorized
            return false;
        }
        //保存用户信息到threadlocal 
        UserHolder.saveUser(user);

        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理ThreadLocal，避免内存泄漏
        UserHolder.removeUser();
    }
}
