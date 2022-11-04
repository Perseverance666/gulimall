package com.example.gulimall.seckill.schedule;

import com.example.gulimall.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
@Component
public class SeckillSkuSchedule {

    @Autowired
    private SeckillService seckillService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void upSeckillSkuLatest3Days(){
        seckillService.upSeckillSkuLatest3Days();
    }
}
