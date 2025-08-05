package com.hmdp.service;
import com.hmdp.dto.UserDTO;
public interface UserStorageStrategy {
    /**
     * 保存验证码
     */
    void saveCode(String phone, String code);
    
    /**
     * 获取验证码
     */
    String getCode(String phone);
    
    /**
     * 保存用户信息
     */
    void saveUser(String key, UserDTO user);
    
    /**
     * 获取用户信息
     */
    UserDTO getUser(String key);
    
    /**
     * 生成用户标识(token或session)
     */
    String generateUserKey();
}
