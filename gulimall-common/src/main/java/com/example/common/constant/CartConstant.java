package com.example.common.constant;

/**
 * @Date: 2022/10/21 16:25
 */

public class CartConstant {
    //cookie中存放的user-key
    public static final String COOKIE_USER_KEY_NAME = "user-key";

    //user-key的存活时间
    public static final int COOKIE_USER_KEY_TIMEOUT = 60 * 60 * 24 * 30;

    //购物车信息存入redis中的key的前缀，value为hash类型
    public static final String CART_PREFIX = "cart:";
}
