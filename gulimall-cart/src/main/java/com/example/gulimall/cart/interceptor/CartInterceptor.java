package com.example.gulimall.cart.interceptor;

import com.example.common.constant.AuthConstant;
import com.example.common.constant.CartConstant;
import com.example.common.vo.MemberRespVo;
import com.example.gulimall.cart.vo.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @Date: 2022/10/21 16:32
 * 在执行目标方法之前，判断用户的登录状态。并封装传递(用户信息)给controller
 */

@Component
public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    /**
     * 目标方法执行之前
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();
        MemberRespVo memberRespVo = (MemberRespVo) request.getSession().getAttribute(AuthConstant.LOGIN_USER);
        if (memberRespVo != null) {
            //1、用户登录，封装userId
            userInfoTo.setUserId(memberRespVo.getId());
        }

        //2、无论是否登录，都封装userKey。先判断cookie中是否有user-key
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(CartConstant.COOKIE_USER_KEY_NAME)) {
                    //2.1、cookie中有user-key，封装userKey
                    userInfoTo.setUserKey(cookie.getValue());
                }
            }
        }

        //2.2、cookie中没有user-key，自己创建userKey，并为cookie中存放user-key
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String userKey = UUID.randomUUID().toString();
            userInfoTo.setUserKey(userKey);

            //为cookie中存放user-key
            Cookie cookie = new Cookie("user-key", userKey);
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConstant.COOKIE_USER_KEY_TIMEOUT);
            response.addCookie(cookie);
        }

        //目标方法执行之前，将用户信息封装到ThreadLocal
        threadLocal.set(userInfoTo);
        return true;
    }
}