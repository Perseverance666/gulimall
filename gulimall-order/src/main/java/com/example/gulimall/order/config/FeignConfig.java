package com.example.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @Date: 2022/10/25 19:07
 *
 * Feign在远程调用之前要构造新请求，丢失原来的请求头，调用很多的拦截器。RequestInterceptor interceptor : requestInterceptors
 * 配置RequestInterceptor，解决Feign远程调用丢失请求头问题，导致cookie丢失
 */

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            /**
             * @param requestTemplate   新请求
             */
            @Override
            public void apply(RequestTemplate requestTemplate) {
                System.out.println("RequestInterceptor线程...."+Thread.currentThread().getId());
                //1、使用RequestContextHolder 来拿到刚进来的这个请求
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if(attributes != null){
                    //2、获取老请求，这里面有cookie
                    HttpServletRequest request = attributes.getRequest();
                    if(request != null){
                        //3、获取cookie的请求头信息
                        String cookie = request.getHeader("Cookie");
                        //4、给新请求同步了老请求的cookie
                        requestTemplate.header("Cookie",cookie);
                    }

                }

            }
        };
    }
}
