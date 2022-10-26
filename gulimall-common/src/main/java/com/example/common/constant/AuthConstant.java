package com.example.common.constant;

/**
 * @Date: 2022/10/26 20:11
 *
 * SMS_CODE_CACHE_PREFIX：存放在redis中的短信验证码前缀
 * LOGIN_USER：用户登录信息经过SpringSession存入redis，key为LOGIN_USER
 */

public class AuthConstant {

    public static final String SMS_CODE_CACHE_PREFIX = "sms:code:";
    public static final String LOGIN_USER = "loginUser";
}
