package com.example.gulimall.seckill.interceptor;

import com.example.common.constant.AuthConstant;
import com.example.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Date: 2022/11/6 17:34
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
        //只有秒杀请求才进行登录拦截
        boolean match = antPathMatcher.match("/seckill", uri);
        if(!match){
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
