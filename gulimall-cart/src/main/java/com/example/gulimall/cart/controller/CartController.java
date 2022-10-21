package com.example.gulimall.cart.controller;

import com.example.common.constant.AuthConstant;
import com.example.gulimall.cart.interceptor.CartInterceptor;
import com.example.gulimall.cart.service.CartService;
import com.example.gulimall.cart.vo.CartItem;
import com.example.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.ExecutionException;

/**
 * @Date: 2022/10/21 16:02
 */

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 浏览器有一个cookie；user-key；标识用户身份，一个月后过期；
     * 如果第一次使用jd的购物车功能，都会给一个临时的用户身份；
     * 浏览器以后保存，每次访问都会带上这个cookie；
     *
     * 登录：session有
     * 没登录：按照cookie里面带来user-key来做。
     * 第一次：如果没有临时用户，帮忙创建一个临时用户。
     *
     * @param model
     * @return
     */
    @GetMapping("/cart_index")
    public String cartListPage(Model model){
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        System.out.println(userInfoTo);

        return "cartList";
    }

    /**
     * 添加到购物车成功
     * @param skuId
     * @param num
     * @param model
     * @return
     */
    @GetMapping("/addToCart.html")
    public String addToCart(@RequestParam("skuId") Long skuId,@RequestParam("num") Integer num,Model model) throws ExecutionException, InterruptedException {
        CartItem cartItem = cartService.addToCart(skuId,num);
        model.addAttribute("cartItem",cartItem);
        return "success";
    }
}
