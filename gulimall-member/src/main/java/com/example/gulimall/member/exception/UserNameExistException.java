package com.example.gulimall.member.exception;

/**
 * @Date: 2022/10/17 18:52
 */


public class UserNameExistException extends RuntimeException{
    public UserNameExistException(){
        super("用户名已存在！");
    }
}
