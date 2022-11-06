package com.example.common.constant;

/**
 * @Date: 2022/11/4 20:18
 */
public class SeckillConstant {

    //秒杀场次信息存入redis中的key的前缀，value为list类型
    public static final String SECKILL_SESSIONS_PREFIX = "seckill:sessions:";

    //秒杀商品信息存入redis中的key的前缀，value为hash类型
    public static final String SECKILL_SKUS_PREFIX = "seckill:skus:";

    //设置秒杀商品分布式信号量作为库存扣减信息，这个前缀后面加的是商品随机码而不是skuId
    public static final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    //分布式锁的key，用于防止重复上架商品
    public static final String SECKILL_UP_LOCK = "seckill:up:lock:";

    //用于秒杀功能中，校验该用户是否买过。只要秒杀成功，就去占位setnx，保证幂等性
    public static final String SECKILL_MEMBER_PREFIX = "seckill:member:";
}
