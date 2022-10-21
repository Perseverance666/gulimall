package com.example.gulimall.cart.vo;

import lombok.Data;

/**
 * @Date: 2022/10/21 16:36
 * 用来存放用户信息
 *   登录：存userId 和 userKey
 *   没登录：存userKey
 */

@Data
public class UserInfoTo {

    /**
     * 用户id
     * 若为空证明没登录
     */
    private Long userId;

    /**
     * 一定封装，无论是否登录
     * 浏览器中cookie存放，用来标识用户身份
     * 只要访问gulimall-cart服务，就会生成user_key，存放到cookie中。cookie中有则不生成 (一个月后过期)
     */
    private String userKey;

}
