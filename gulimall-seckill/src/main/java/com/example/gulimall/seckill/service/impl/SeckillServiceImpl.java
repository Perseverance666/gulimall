package com.example.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.common.constant.SeckillConstant;
import com.example.common.exception.RRException;
import com.example.common.utils.R;
import com.example.gulimall.seckill.config.RedissonConfig;
import com.example.gulimall.seckill.feign.CouponFeignService;
import com.example.gulimall.seckill.feign.ProductFeignService;
import com.example.gulimall.seckill.service.SeckillService;
import com.example.gulimall.seckill.to.SecKillSkuRedisTo;
import com.example.gulimall.seckill.to.SeckillSessionWithSkusTo;
import com.example.gulimall.seckill.to.SeckillSkuTo;
import com.example.gulimall.seckill.to.SkuInfoTo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @Date: 2022/11/4 19:04
 */

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private CouponFeignService couponFeignService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private RedissonClient redissonClient;


    /**
     * 上架最近三天需要秒杀的商品
     */
    @Override
    public void upSeckillSkuLatest3Days() {
        //1、远程调用，获取未来三天的秒杀场次
        R r = couponFeignService.getSeckillSessionLatest3Days();
        if(r.getCode() != 0){
            throw new RRException("远程调用coupon的getSeckillSessionLatest3Days失败");
        }
        List<SeckillSessionWithSkusTo> sessions = r.getData("data", new TypeReference<List<SeckillSessionWithSkusTo>>() {});
        if(sessions != null){
            //2、上架商品
            //2.1、缓存秒杀场次
            saveRedisSeckillSessions(sessions);
            //2.2、缓存秒杀商品
            saveRedisSeckillSkus(sessions);

        }
    }

    /**
     * 缓存秒杀场次。value为list结构: key -> seckill:sessions:startTime_endTime  value -> [promotionSessionId_skuId]
     * @param sessions
     */
    private void saveRedisSeckillSessions(List<SeckillSessionWithSkusTo> sessions) {
        sessions.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime(); //存入redis转为Long类型
            Long endTime = session.getEndTime().getTime();
            //1、key -> seckill:sessions:startTime_endTime
            String key = SeckillConstant.SECKILL_SESSIONS_PREFIX +startTime+ "_"+endTime;
            Boolean hasKey = redisTemplate.hasKey(key);
            if(!hasKey){
                //2、value -> [promotionSessionId_skuId]
                List<String> value = session.getRelationSkus().stream().map(relationSku -> {
                    return relationSku.getPromotionSessionId().toString() + "_" + relationSku.getSkuId().toString();
                }).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key,value);
            }

        });
    }

    /**
     * 缓存秒杀商品。value为hash结构: key -> seckill:skus: value -> (key->promotionSessionId_skuId ,value->SecKillSkuRedisTo)
     * @param sessions
     */
    private void saveRedisSeckillSkus(List<SeckillSessionWithSkusTo> sessions) {
        sessions.stream().forEach(session -> {
            //1、准备hash操作
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SeckillConstant.SECKILL_SKUS_PREFIX);
            session.getRelationSkus().stream().forEach(relationSku -> {
                //2、封装redisTo
                SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();

                //2.1、封装seckillSkuTo
                redisTo.setSeckillSku(relationSku);

                //2.2、封装skuInfoTo
                R r = productFeignService.getSkuInfo(relationSku.getSkuId());
                if(r.getCode() != 0){
                    throw new RRException("远程调用product的getSkuInfo失败");
                }
                SkuInfoTo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoTo>() {});
                redisTo.setSkuInfo(skuInfo);

                //2.3、封装当前商品秒杀的开始时间和结束时间
                redisTo.setStartTime(session.getStartTime().getTime());
                redisTo.setEndTime(session.getEndTime().getTime());

                //2.4、封装商品秒杀随机码
                String token = UUID.randomUUID().toString().replace("-", "");
                redisTo.setRandomCode(token);

                //2.5、使用库存作为分布式的信号量，用于限流。
                RSemaphore semaphore = redissonClient.getSemaphore(SeckillConstant.SKU_STOCK_SEMAPHORE + token);
                //设置商品可以秒杀的数量作为信号量
                semaphore.trySetPermits(relationSku.getSeckillCount());

                //3、将redisTo转为json存入redis中
                String jsonString = JSON.toJSONString(redisTo);
                String key = relationSku.getPromotionSessionId().toString() + "_" + relationSku.getSkuId().toString();
                hashOps.put(key,jsonString);

            });
        });
    }




}
