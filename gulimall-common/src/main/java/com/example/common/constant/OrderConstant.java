package com.example.common.constant;

/**
 * @Date: 2022/10/26 20:11
 *
 * USER_ORDER_TOKEN_PREFIX：redis中存放令牌的前缀，用于防止订单重复提交
 */
public class OrderConstant {
    public static final String USER_ORDER_TOKEN_PREFIX = "order:token:";
}
