package com.example.gulimall.cart.service;

import com.example.gulimall.cart.vo.Cart;
import com.example.gulimall.cart.vo.CartItem;

import java.util.concurrent.ExecutionException;

/**
 * @Date: 2022/10/21 16:04
 */

public interface CartService {
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItem getCartItem(Long skuId);

    Cart getCart() throws ExecutionException, InterruptedException;

    void checkItem(Long skuId, Integer check);

    void countItem(Long skuId, Integer num);

    void deleteItem(Long skuId);
}
