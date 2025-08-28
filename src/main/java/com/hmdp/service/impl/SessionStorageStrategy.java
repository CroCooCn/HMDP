package com.hmdp.service.impl;

import static com.hmdp.utils.SessionConstants.CODE_PREF;

import javax.servlet.http.HttpSession;

import com.hmdp.dto.UserDTO;
import com.hmdp.service.UserStorageStrategy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cn.hutool.core.bean.BeanUtil;

// @Component 注解的作用是将当前类交给Spring容器管理，成为一个Spring Bean，便于在项目中通过依赖注入的方式使用该类。
@Component
@ConditionalOnProperty(name = "app.cache.login",havingValue = "session")
public class SessionStorageStrategy implements UserStorageStrategy{
    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) 
            RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession();
    }



    @Override
    public void saveCode(String phone,String code) {
        //将"手机号-验证码"保存到session中
        getSession().setAttribute(CODE_PREF+phone, code);
    }

    /**
     * 获取验证码
     */
    @Override
    public String getCode(String phone){
        Object code=getSession().getAttribute(CODE_PREF+phone);
        if(code==null) return null;
        return code.toString();
    }
    
    /**
     * 保存用户信息
     */
    @Override
    public void saveUser(String key, UserDTO user){
       getSession().setAttribute("cur_user", user);
    }
    
    /**
     * 获取用户信息
     */
    @Override
    public UserDTO getUser(String key){
       Object user = getSession().getAttribute("cur_user");
        if (user == null) {
            return null;
        }
        // 使用BeanUtil将user属性拷贝到UserDTO对象
        return BeanUtil.copyProperties(user, UserDTO.class);
    }
    
    /**
     * 生成用户标识(token或session)
     */
    @Override
    public String generateUserKey(){
        return "session";
    }
}
