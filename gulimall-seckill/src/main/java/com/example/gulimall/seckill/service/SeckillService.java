package com.example.gulimall.seckill.service;

import com.example.gulimall.seckill.to.SecKillSkuRedisTo;

import java.util.List;

/**
 * @Date: 2022/11/4 19:04
 */
public interface SeckillService {
    void upSeckillSkuLatest3Days();

    List<SecKillSkuRedisTo> getCurrentSeckillSkus();

    SecKillSkuRedisTo getSkuSeckillInfo(Long skuId);
}
