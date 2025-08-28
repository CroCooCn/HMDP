package com.hmdp.service.impl;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.hmdp.dto.UserDTO;
import com.hmdp.service.UserStorageStrategy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import lombok.extern.slf4j.Slf4j;

// @Component 注解的作用是将当前类交给Spring容器管理，成为一个Spring Bean，便于在项目中通过依赖注入的方式使用该类。
@Component
@Slf4j
@ConditionalOnProperty(name = "app.cache.login",havingValue = "redis")
public class RedisStorageStrategy implements UserStorageStrategy{
  @Resource
  private StringRedisTemplate stringRedisTemplate;
  
  @Override
  public void saveCode(String phone,String code) {
    //将"手机号-验证码"保存到redis中
    stringRedisTemplate.opsForValue().set(
        LOGIN_CODE_KEY+phone,
        code,
        LOGIN_CODE_TTL,
        TimeUnit.MINUTES
    );
  }

    /**
     * 获取验证码
     */
    @Override
    public String getCode(String phone){
        return stringRedisTemplate.opsForValue().get(
            LOGIN_CODE_KEY+phone
        );
    }
    
    /**
     * 保存用户信息
     */
    @Override
    public void saveUser(String key, UserDTO user){
        Map<String, Object> userMap = BeanUtil.beanToMap(user,new HashMap<>(),
        CopyOptions.create().setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+key, userMap);
        //设置超时时间
        stringRedisTemplate.expire(LOGIN_USER_KEY+key, LOGIN_USER_TTL,TimeUnit.MINUTES);
    }
    
    /**
     * 获取用户信息
     */
    @Override
    public UserDTO getUser(String token){
        Map<Object,Object> userMap = stringRedisTemplate.opsForHash()
                    .entries(LOGIN_USER_KEY+token);;
        if(userMap.isEmpty()) {
            log.debug("在redis中没有找到该用户！");
            return null;
        }else {
            
            UserDTO FoundUser = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false,true);
            log.debug("在redis中找到了用户：");
            log.debug(FoundUser.toString());
            return FoundUser;
        }


        }
    
    /**
     * 生成用户标识(token)(仅redis使用)
     */
    @Override
    public String generateUserKey(){
        return UUID.randomUUID().toString(true);
    }
}
