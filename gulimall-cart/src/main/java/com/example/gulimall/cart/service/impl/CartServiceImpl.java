package com.example.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.common.constant.CartConstant;
import com.example.common.exception.RRException;
import com.example.common.utils.R;
import com.example.gulimall.cart.feign.ProductFeignService;
import com.example.gulimall.cart.interceptor.CartInterceptor;
import com.example.gulimall.cart.service.CartService;
import com.example.gulimall.cart.vo.Cart;
import com.example.gulimall.cart.vo.CartItem;
import com.example.gulimall.cart.vo.SkuInfoTo;
import com.example.gulimall.cart.vo.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

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
     * 删除指定购物项
     * @param skuId
     */
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> hashOps = getCartOps();
        hashOps.delete(skuId.toString());
    }


    /**
     * 改变购物项的数量
     * @param skuId
     * @param num
     */
    @Override
    public void countItem(Long skuId, Integer num) {
        //1、改变购物项的数量
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        //2、更新redis中数据
        BoundHashOperations<String, Object, Object> hashOps = getCartOps();
        String json = JSON.toJSONString(cartItem);
        hashOps.put(skuId.toString(),json);
    }


    /**
     * 改变购物项的选中状态
     * @param skuId
     * @param check
     */
    @Override
    public void checkItem(Long skuId, Integer check) {
        //1、改变购物项的选中状态
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1);
        //2、更新redis中数据
        BoundHashOperations<String, Object, Object> hashOps = getCartOps();
        String json = JSON.toJSONString(cartItem);
        hashOps.put(skuId.toString(),json);
    }

    /**
     * 获取购物车所有信息
     * @return
     */
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if(userInfoTo.getUserId() == null){
            //-----没登录
            //去redis中查询所有购物项信息
            String cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);

        }else{
            //-----登录
            //1、判断临时用户的购物车中是否有数据
            String tempCartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if(tempCartItems != null){
                //1.1、若临时购物车有数据，数据进行合并
                for (CartItem tempCartItem : tempCartItems) {
                    addToCart(tempCartItem.getSkuId(),tempCartItem.getCount());
                }
                //1.2、清空临时购物车中数据
                redisTemplate.delete(tempCartKey);

            }
            //2.去redis中查询该用户的所有购物项信息
            String cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);

        }
        return cart;
    }


    /**
     * 获取指定用户购物车中的所有购物项
     * @param cartKey
     * @return
     */
    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        List<CartItem> cartItems = values.stream().map((value) -> {
            String json = (String) value;
            CartItem cartItem = JSON.parseObject(json, CartItem.class);
            return cartItem;
        }).collect(Collectors.toList());

        return cartItems;

    }

    /**
     * 获取购物车中某个购物项
     * @param skuId
     * @return
     */
    @Override
    public CartItem getCartItem(Long skuId) {
        //1、获取指定用户的购物车
        BoundHashOperations<String, Object, Object> hashOps = getCartOps();
        //2、获取redis中存放购物项信息
        String json = (String) hashOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(json, CartItem.class);
        return cartItem;
    }


    /**
     * 将商品添加到购物车
     * @param skuId
     * @param num
     * @return
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        //1、获取指定用户的购物车
        BoundHashOperations<String, Object, Object> hashOps = getCartOps();

        //2、先判断该用户的购物车是否有该商品
        String json = (String) hashOps.get(skuId.toString());
        if(StringUtils.isEmpty(json)){
            //2.1、购物车中没有该商品
            CartItem cartItem = new CartItem();
            //封装CartItem。远程调用采用异步编排
            //2.1.1、远程调用查询sku信息
            CompletableFuture<Void> skuInfoFuture = CompletableFuture.runAsync(() -> {
                R r = productFeignService.getSkuInfo(skuId);
                if (r.getCode() == 0) {
                    SkuInfoTo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoTo>() {});
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

            //2.1.2、远程调用查询sku的销售属性
            CompletableFuture<Void> skuSaleAttrsFuture = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrs = productFeignService.getSkuSaleAttrValues(skuId);
                if (skuSaleAttrs != null) {
                    cartItem.setSkuAttrs(skuSaleAttrs);
                } else {
                    throw new RRException("远程调用getSkuSaleAttrValues异常");
                }
            }, executor);

            //2.1.3、等待所有任务都完成，再将cartItem存入redis
            CompletableFuture.allOf(skuInfoFuture,skuSaleAttrsFuture).get();

            //2.1.4、先将cartItem转为json，避免存入redis时的序列化
            hashOps.put(skuId.toString(),JSON.toJSONString(cartItem));

            return cartItem;
        }else {
            //2.2、购物车中有该商品
            CartItem cartItem = JSON.parseObject(json, CartItem.class);
            //2.2.1、改变购物车中商品数量
            cartItem.setCount(cartItem.getCount() + num);
            //2.2.2、同时更新redis中购物车数据
            hashOps.put(skuId.toString(),JSON.toJSONString(cartItem));
            return cartItem;
        }

    }

    /**
     *  绑定一个hash操作，以后对redis的所有操作，都是对指定key，即指定用户的操作
     *  获取指定用户的购物车redis信息
     *   1.登录：   key -> cart:userId    value -> Map<skuId,CartItem>
     *   2.未登录： key -> cart:userKey   value -> Map<skuId,CartItem>
     */
    public BoundHashOperations<String, Object, Object> getCartOps(){
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if(userInfoTo.getUserId() == null){
            //未登录   key -> cart:userKey
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
        }else{
            //登录    key -> cart:userId
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
        }

        //绑定一个hash操作，以后对redis的所有操作，都是对指定key的操作
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);

        return hashOps;

    }
}
