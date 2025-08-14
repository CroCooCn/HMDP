package com.hmdp.service.impl;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.service.UserStorageStrategy;
import com.hmdp.utils.RegexUtils;

import org.springframework.stereotype.Service;

import cn.hutool.core.bean.BeanUtil;
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
    private UserStorageStrategy storageStrategy;
    
    @Override
    public Result sendCode(String phone,HttpSession  session) {
        //去处手机号开头结尾的空格
        phone=phone.trim();

        //检查手机号码格式
        if(RegexUtils.isPhoneInvalid(phone))
            return Result.fail("手机号格式错误");
        //生成验证码
        String code=RandomUtil.randomNumbers(6);
        //保存验证码到session/redis
        storageStrategy.saveCode(phone,code);
        //先不真的发送验证码

        log.debug("发送验证码成功：{}",code);
        //运行到这里，说明没有问题
        return Result.ok();
    }
    
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //手机号是否正确
        String phone=loginForm.getPhone().trim();
        if(RegexUtils.isPhoneInvalid(phone))
            return Result.fail("手机号格式错误");
        //该手机号是否被发送了验证码，验证码是否正确
        String savedCode=storageStrategy.getCode(phone);
        String inputCode=loginForm.getCode().trim();
        if(savedCode==null || !savedCode.equals(inputCode)) {
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
            log.debug("注册了新用户,存在数据库中,phone:"+phone);
        }else {
            log.debug("用户{}在数据库中已存在",phone);
        }
        //保存用户登录状态: redis-token session-无
        String userKey=storageStrategy.generateUserKey();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        storageStrategy.saveUser(userKey,userDTO);
        return Result.ok(userKey);  //保存token到本地浏览器

    }

}
