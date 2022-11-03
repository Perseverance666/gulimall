package com.example.gulimall.order.interceptor;

import com.example.common.constant.AuthConstant;
import com.example.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Date: 2022/10/25 15:50
 *
 * 登录拦截器
 */

@Component
public class LoginInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        //MQ监听器执行远程调用order的getOrderByOrderSn,无法获取cookie，无法判断是否登录，故不进行拦截
        boolean match = antPathMatcher.match("/order/order/orderSn/**", uri);
        //支付宝的异步通知，不进行拦截
        boolean match1 = antPathMatcher.match("/payed/notify", uri);
        if(match || match1){
            return true;
        }


        MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthConstant.LOGIN_USER);
        if(attribute == null){
            //没登录，返回登录页面
            request.getSession().setAttribute("msg","请先进行登录：");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }else{
            loginUser.set(attribute);
            return true;
        }
    }
}
