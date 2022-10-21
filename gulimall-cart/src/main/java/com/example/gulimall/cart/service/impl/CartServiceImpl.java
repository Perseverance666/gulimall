package com.example.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.common.constant.CartConstant;
import com.example.common.exception.RRException;
import com.example.common.utils.R;
import com.example.gulimall.cart.feign.ProductFeignService;
import com.example.gulimall.cart.interceptor.CartInterceptor;
import com.example.gulimall.cart.service.CartService;
import com.example.gulimall.cart.vo.CartItem;
import com.example.gulimall.cart.vo.SkuInfoTo;
import com.example.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Date: 2022/10/21 16:05
 */

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private ThreadPoolExecutor executor;

    /**
     * 添加到购物车
     * @param skuId
     * @param num
     * @return
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        //1、获取指定用户的购物车
        BoundHashOperations<String, Object, Object> hashOps = getCart();

        CartItem cartItem = new CartItem();

        //2、封装CartItem。远程调用采用异步编排
        //2。1、远程调用查询sku信息
        CompletableFuture<Void> skuInfoFuture = CompletableFuture.runAsync(() -> {
            R r = productFeignService.getSkuInfo(skuId);
            if (r.getCode() == 0) {
                SkuInfoTo skuInfo = (SkuInfoTo) r.get("skuInfo");
                cartItem.setSkuId(skuId);
                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setImage(skuInfo.getSkuDefaultImg());
                cartItem.setPrice(skuInfo.getPrice());
                cartItem.setTitle(skuInfo.getSkuTitle());
            } else {
                throw new RRException("远程调用getSkuInfo异常");
            }
        }, executor);

        //2.2、远程调用查询sku的销售属性
        CompletableFuture<Void> skuSaleAttrsFuture = CompletableFuture.runAsync(() -> {
            List<String> skuSaleAttrs = productFeignService.getSkuSaleAttrValues(skuId);
            if (skuSaleAttrs != null) {
                cartItem.setSkuAttrs(skuSaleAttrs);
            } else {
                throw new RRException("远程调用getSkuSaleAttrValues异常");
            }
        }, executor);

        CompletableFuture.allOf(skuInfoFuture,skuSaleAttrsFuture).get();

        //现将cartItem转为json，避免存入redis时的序列化过程
        String jsonString = JSON.toJSONString(cartItem);
        hashOps.put(skuId.toString(),jsonString);

        return cartItem;
    }

    /**
     * 获取指定用户的购物车redis信息
     *   1.登录：   key -> cart:userId    value -> Map<skuId,CartItem>
     *   2.未登录： key -> cart:userKey   value -> Map<skuId,CartItem>
     */
    public BoundHashOperations<String, Object, Object> getCart(){
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if(userInfoTo.getUserId() == null){
            //未登录
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
        }else{
            //登录
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
        }

        //绑定一个hash操作，以后对redis的所有操作，都是对指定key的操作
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);

        return hashOps;

    }
}
