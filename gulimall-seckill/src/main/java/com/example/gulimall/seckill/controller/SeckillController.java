package com.example.gulimall.seckill.controller;

import com.example.common.utils.R;
import com.example.gulimall.seckill.service.SeckillService;
import com.example.gulimall.seckill.to.SecKillSkuRedisTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Date: 2022/11/5 20:05
 */

@Slf4j
@RestController
public class SeckillController {
    @Autowired
    private SeckillService seckillService;

    /**
     * 获取当前时间可以参与的秒杀商品信息
     * @return
     */
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus(){
        log.info("currentSeckillSkus正在执行...");
        List<SecKillSkuRedisTo> redisTos = seckillService.getCurrentSeckillSkus();
        return R.ok().put("data",redisTos);
    }

    /**
     * 获取当前sku的秒杀信息
     * @param skuId
     * @return
     */
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId){
        SecKillSkuRedisTo secKillSkuRedisTo = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().put("data",secKillSkuRedisTo);
    }
}
