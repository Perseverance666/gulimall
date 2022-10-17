package com.example.gulimall.member.exception;

/**
 * @Date: 2022/10/17 18:53
 */


public class PhoneExistException extends RuntimeException{
    public PhoneExistException(){
        super("该手机号已注册！");
    }
}
