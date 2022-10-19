package com.example.gulimall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Date: 2022/10/16 20:27
 */

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 视图映射
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        //写controller了，loginPage处理登录页逻辑
//        registry.addViewController("login.html").setViewName("login");
        registry.addViewController("register.html").setViewName("register");
    }
}
