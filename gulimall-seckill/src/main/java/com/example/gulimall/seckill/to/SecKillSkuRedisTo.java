package com.example.gulimall.seckill.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Date: 2022/11/4 20:36
 * 秒杀商品存放hash类型redis中value的to
 */
@Data
public class SecKillSkuRedisTo {
    //秒杀商品信息
    private SeckillSkuTo seckillSku;

    //sku的详细信息
    private SkuInfoTo skuInfo;

    //当前商品秒杀的开始时间
    private Long startTime;

    //当前商品秒杀的结束时间
    private Long endTime;

    //商品秒杀随机码
    private String randomCode;





}
