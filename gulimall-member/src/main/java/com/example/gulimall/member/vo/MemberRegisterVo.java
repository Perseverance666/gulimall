package com.example.gulimall.member.vo;

import lombok.Data;

/**
 * @Date: 2022/10/17 17:52
 * 用于将注册信息写入到ums_member表中的vo
 *
 */

@Data
public class MemberRegisterVo {

    private String username;

    private String password;

    private String phone;
}
