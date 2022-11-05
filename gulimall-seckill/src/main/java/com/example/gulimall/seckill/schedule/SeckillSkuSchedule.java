package com.example.gulimall.seckill.schedule;

import com.example.common.constant.SeckillConstant;
import com.example.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Date: 2022/11/4 19:00
 *
 * 秒杀商品的定时上架；
 *     每天晚上3点；上架最近三天需要秒杀的商品。
 *     当天00:00:00  - 23:59:59
 *     明天00:00:00  - 23:59:59
 *     后天00:00:00  - 23:59:59
 */

@EnableAsync
@EnableScheduling
@Slf4j
@Component
public class SeckillSkuSchedule {

    @Autowired
    private SeckillService seckillService;
    @Autowired
    private RedissonClient redissonClient;

    @Scheduled(cron = "*/10 * * * * ?")
    public void upSeckillSkuLatest3Days(){
        //1、重复上架无需处理
        // 分布式锁。锁的业务执行完成，状态已经更新完成。释放锁以后。其他人获取到就会拿到最新的状态。
        RLock lock = redissonClient.getLock(SeckillConstant.SECKILL_UP_LOCK);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.upSeckillSkuLatest3Days();
        }finally {
            lock.unlock();
        }

    }
}
