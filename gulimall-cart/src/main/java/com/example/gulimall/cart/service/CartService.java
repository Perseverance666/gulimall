package com.example.gulimall.cart.service;

import com.example.gulimall.cart.vo.CartItem;

import java.util.concurrent.ExecutionException;

/**
 * @Date: 2022/10/21 16:04
 */

public interface CartService {
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;
}
