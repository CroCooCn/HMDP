package com.hmdp.utils;

import javax.servlet.http.*;

import org.springframework.web.servlet.HandlerInterceptor;

import com.hmdp.dto.UserDTO;

public class LoginInterceptor implements HandlerInterceptor{
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        Object user = session.getAttribute("cur_user");
        //用户不存在，进行拦截
        if(user==null) {
            response.setStatus(401);    //401-unauthorized
            return false;
        }
        //保存用户信息到threadlocal - session中已经是UserDTO
        UserDTO userDTO = (UserDTO)user;
        UserHolder.saveUser(userDTO);

        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理ThreadLocal，避免内存泄漏
        UserHolder.removeUser();
    }
}
