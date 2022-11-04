package com.example.common.constant;

/**
 * @Date: 2022/11/4 20:18
 */
public class SeckillConstant {

    //秒杀场次信息存入redis中的key的前缀，value为list类型
    public static final String SECKILL_SESSIONS_PREFIX = "seckill:sessions:";

    //秒杀商品信息存入redis中的key的前缀，value为hash类型
    public static final String SECKILL_SKUS_PREFIX = "seckill:skus:";

    //后面加商品随机码而不是skuId
    public static final String SKU_STOCK_SEMAPHORE = "seckill:stock:";
}
