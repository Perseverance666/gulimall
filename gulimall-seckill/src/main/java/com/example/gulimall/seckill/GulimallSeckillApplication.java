package com.example.gulimall.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 1、秒杀(高并发)系统关注的问题:
 *    1)、服务单一职责 + 独立部署：秒杀服务即使自己扛不住压力，挂掉，也不能影响其他服务
 *    2)、秒杀链接加密：给秒杀商品设置随机码，到秒杀时间才对外暴露，拿着随机码才能进行秒杀。防止恶意攻击，防止链接暴露
 *    3)、库存预热 + 快速扣减：定时任务上架后，秒杀商品的库存以信号量形式放入redis，做到原子减量。
 *                          秒杀读多写少，无需每次实时校验库存。库存预热，放入redis中。信号量控制进来秒杀的请求
 *    4)、动静分离：nginx做好动静分离，保证秒杀和商品详情页的动态请求才打到后端的服务集群。使用CDN网络，分担本集群压力
 *    5)、恶意请求拦截：识别非法攻击请求并进行拦截，网关层解决
 *    6)、流量错峰：使用各种手段将流量分担到更大宽度的时间点。比如验证码、加入购物车
 *    7)、限流、熔断、降级：前端限流+后端限流、限制次数、限制总量，快速失败降级运行，熔断隔离防止雪崩
 *    8)、队列削峰：所有秒杀成功的请求进入队列，慢慢创建订单，扣减库存即可
 *
 * 2、
 */


@EnableRedisHttpSession
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GulimallSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallSeckillApplication.class, args);
    }

}
