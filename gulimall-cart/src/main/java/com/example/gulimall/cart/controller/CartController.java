package com.example.gulimall.cart.controller;

import com.example.common.constant.AuthConstant;
import com.example.gulimall.cart.interceptor.CartInterceptor;
import com.example.gulimall.cart.service.CartService;
import com.example.gulimall.cart.vo.Cart;
import com.example.gulimall.cart.vo.CartItem;
import com.example.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        Cart cart = cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";
    }

    /**
     * 添加到购物车
     * @param skuId
     * @param num
     * @return
     */
    @GetMapping("/gate.action")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) throws ExecutionException, InterruptedException {
        //添加到购物车
        cartService.addToCart(skuId, num);
        //重定向到添加成功页面
        return "redirect:http://cart.gulimall.com/addToCart.html?skuId="+skuId+"&num="+num;

    }

    /**
     * 跳转到添加购物车成功页面
     * @return
     */
    @GetMapping("/addToCart.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,@RequestParam("num") Integer num,Model model){
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("cartItem",cartItem);
        return "success";
    }

    /**
     * 改变购物项的选中状态
     * @param skuId
     * @param check
     * @return
     */
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,@RequestParam("check") Integer check){
        cartService.checkItem(skuId,check);

        return "redirect:http://cart.gulimall.com/cart_index";
    }

    /**
     * 改变购物项的数量
     * @param skuId
     * @param num
     * @return
     */
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,@RequestParam("num") Integer num){
        cartService.countItem(skuId,num);
        return "redirect:http://cart.gulimall.com/cart_index";
    }

    /**
     * 删除指定购物项
     * @param skuId
     * @return
     */
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart_index";
    }
}
