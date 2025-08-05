package com.hmdp.service.impl;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SmsUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;




/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private SmsUtils smsUtils;
    
    @Override
    public Result sendCode(String phone,HttpSession  session) {
        //检查手机号码格式
        if(RegexUtils.isPhoneInvalid(phone))
            return Result.fail("手机号格式错误");
        //生成验证码
        String code=RandomUtil.randomNumbers(6);
        //save code
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL,TimeUnit.MINUTES);
        //不真的发送验证码

        log.debug("发送验证码成功：{}",code);
        //运行到这里，说明没有问题
        return Result.ok();
    }
    
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //手机号是否正确
        String phone=loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone))
            return Result.fail("手机号格式错误");
        //该手机号是否被发送了验证码，验证码是否正确
        Object cor_code=session.getAttribute(phone);
        String code=loginForm.getCode();
        if(cor_code==null || !cor_code.toString().equals(code)) {
            return Result.fail("验证码错误");
        }
        //如果用户还未创建，先进行注册
        User user=query().eq("phone",phone).one();
        if(user==null) {
            User nuser=new User();
            nuser.setPhone(phone);
            nuser.setNickName(RandomUtil.randomString(10));
            save(nuser);
            user=nuser;
        }
        //保存用户信息（token格式）到redis中
        //session.setAttribute("cur_user", BeanUtil.toBean(user, UserDTO.class));
        String token=UUID.randomUUID().toString(true);
        System.out.println(token);
        return Result.ok();
    }

}
