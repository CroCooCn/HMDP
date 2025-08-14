package com.hmdp.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
    
    @Override
    public String toString() {
        return "用户信息 [" +
                "id=" + id +
                ", 昵称='" + nickName + '\'' +
                ", 头像='" + icon + '\'' +
                ']';
    }
}
