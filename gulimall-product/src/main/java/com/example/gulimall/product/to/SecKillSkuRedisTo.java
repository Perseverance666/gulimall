package com.example.gulimall.product.to;

import lombok.Data;

/**
 * @Date: 2022/11/4 20:36
 * 秒杀商品存放hash类型redis中value的to
 */
@Data
public class SecKillSkuRedisTo {
    //秒杀商品信息
    private SeckillSkuTo seckillSku;

    //当前商品秒杀的开始时间
    private Long startTime;

    //当前商品秒杀的结束时间
    private Long endTime;

    /**
     * 商品秒杀随机码
     * 只有该商品在秒杀时间内，才对外显示
     */
    private String randomCode;





}
