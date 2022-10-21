package com.example.gulimall.cart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * @Date: 2022/10/19 18:23
 * 自定义SpringSession配置
 *
 * SpringSession设置domain解决分布式问题
 */

@Configuration
public class SessionConfig {

    /**
     * 解决子域session共享问题
     * @return
     */
    @Bean
    public CookieSerializer cookieSerializer(){
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        cookieSerializer.setCookieName("GULIMALLSESSION");
        //解决子域session共享
        cookieSerializer.setDomainName("gulimall.com");
        return cookieSerializer;
    }

    /**
     * 使用JSON的序列化方式来序列化对象数据到redis中
     * @return
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

}
