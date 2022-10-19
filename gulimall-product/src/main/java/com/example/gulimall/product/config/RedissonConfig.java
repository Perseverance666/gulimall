package com.example.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Date: 2022/10/8 15:22
 */

@Configuration
public class RedissonConfig {

    /**
     * 所有对Redisson的使用都是通过RedissonClient对象
     * Redisson中的方法都具有原子性
     * @return
     */
    @Bean(destroyMethod="shutdown")
    public RedissonClient redissonClient(){
        //1、创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.138.102:6379");
        //2、根据Config创建出RedissonClient示例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
