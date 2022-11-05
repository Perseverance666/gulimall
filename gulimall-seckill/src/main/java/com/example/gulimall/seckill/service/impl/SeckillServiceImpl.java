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
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Date: 2022/11/4 19:04
 */

@Slf4j
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
     * 获取当前sku的秒杀信息
     * @param skuId
     * @return
     */
    @Override
    public SecKillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        //1、先去redis中去查所有秒杀商品信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SeckillConstant.SECKILL_SKUS_PREFIX);
        Set<String> keys = hashOps.keys();
        if(keys != null && keys.size() > 0){
            //2、使用正则表达式来匹配当前sku的秒杀商品信息
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if(Pattern.matches(regx,key)){
                    String json = hashOps.get(key);
                    SecKillSkuRedisTo to = JSON.parseObject(json, SecKillSkuRedisTo.class);
                    //3、判断当前时间是否是该sku的秒杀时间，若不是，它的随机码要设置为null，只有在秒杀时间内，随机码才暴露给外面
                    Long now = new Date().getTime();
                    if(now < to.getStartTime() || now > to.getEndTime()){
                        to.setRandomCode(null);
                    }
                    return to;
                }
            }
        }

        return null;
    }

    /**
     * 获取当前时间可以参与的秒杀商品信息
     * @return
     */
    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        //1、确定当前时间属于哪个秒杀场次
        Long now = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SeckillConstant.SECKILL_SESSIONS_PREFIX + "*");
        for (String key : keys) {
            //seckill:sessions:1582250400000_1582254000000
            String replace = key.replace(SeckillConstant.SECKILL_SESSIONS_PREFIX, "");
            String[] s = replace.split("_");
            Long startTime = Long.parseLong(s[0]);
            Long endTime = Long.parseLong(s[1]);
            if(now >= startTime && now <= endTime){
                //2、获取这个秒杀场次需要的所有商品信息
                //2.1、从seckill:session中获取所有list类型的值,[promotionSessionId_skuId]
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                //2.2、从seckill:skus中获取所有商品信息
                BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SeckillConstant.SECKILL_SKUS_PREFIX);
                List<String> list = ops.multiGet(range);
                if(list != null){
                    List<SecKillSkuRedisTo> collect = list.stream().map(item -> {
                        SecKillSkuRedisTo secKillSkuRedisTo = JSON.parseObject(item, SecKillSkuRedisTo.class);
                        return secKillSkuRedisTo;
                    }).collect(Collectors.toList());
                    //找到当前时间属于哪个秒杀场次，不用再遍历去找了
                    return collect;
                }
            }
        }

        return null;
    }


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
            log.info("上架未来3天的秒杀商品...");
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
            //确保幂等性
            if(!redisTemplate.hasKey(key)){
                //2、value -> [promotionSessionId_skuId]
                List<String> value = session.getRelationSkus().stream().map(relationSku -> {
                    return relationSku.getPromotionSessionId() + "_" + relationSku.getSkuId();
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
                //生成每个秒杀商品的随机码
                String token = UUID.randomUUID().toString().replace("-", "");

                //确保幂等性
                if(!hashOps.hasKey(relationSku.getPromotionSessionId()+ "_" + relationSku.getSkuId())){
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

                    //2.4、封装商品秒杀随机码，每个商品都有它固定的随机码
                    redisTo.setRandomCode(token);

                    //3、将redisTo转为json存入redis中
                    String jsonString = JSON.toJSONString(redisTo);
                    String key = relationSku.getPromotionSessionId() + "_" + relationSku.getSkuId();
                    hashOps.put(key,jsonString);

                    //4、设置秒杀商品分布式信号量作为库存扣减信息 ，用于限流。
                    RSemaphore semaphore = redissonClient.getSemaphore(SeckillConstant.SKU_STOCK_SEMAPHORE + token);
                    //设置商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(relationSku.getSeckillCount());

                }

            });
        });
    }




}
