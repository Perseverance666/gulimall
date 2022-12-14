package com.example.gulimall.seckill.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.common.constant.SeckillConstant;
import com.example.common.exception.RRException;
import com.example.common.to.mq.SeckillOrderTo;
import com.example.common.utils.R;
import com.example.common.vo.MemberRespVo;
import com.example.gulimall.seckill.config.RedissonConfig;
import com.example.gulimall.seckill.feign.CouponFeignService;
import com.example.gulimall.seckill.feign.ProductFeignService;
import com.example.gulimall.seckill.interceptor.LoginInterceptor;
import com.example.gulimall.seckill.service.SeckillService;
import com.example.gulimall.seckill.to.SecKillSkuRedisTo;
import com.example.gulimall.seckill.to.SeckillSessionWithSkusTo;
import com.example.gulimall.seckill.to.SeckillSkuTo;
import com.example.gulimall.seckill.to.SkuInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * ????????????
     * <p>
     * ????????????(???????????????) -> ???redis??????????????????????????? -> ???????????????????????? -> ??????????????????id???????????? -> ??????????????????
     * -> ??????????????????????????? -> ???????????? -> ??????????????????mq?????? -> ????????????????????????????????????????????????,??????????????????,????????????
     * <p>
     * ???????????????????????????????????????????????????????????????setnx??????????????????
     * key->seckill:member:userId_promotionSessionId_skuId, value -> ???????????????num, ???????????????????????????????????????-????????????
     *
     *  TODO ?????????????????????????????????????????????????????????
     * @param seckillId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String seckill(String seckillId, String key, Integer num) {
        long s1 = System.currentTimeMillis();
        //1???redis???????????????????????????
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SeckillConstant.SECKILL_SKUS_PREFIX);
        String json = hashOps.get(seckillId);
        if (!StringUtils.isEmpty(json)) {
            SecKillSkuRedisTo redisTo = JSON.parseObject(json, SecKillSkuRedisTo.class);
            //2???????????????????????????
            Long now = new Date().getTime();
            Long startTime = redisTo.getStartTime();
            Long endTime = redisTo.getEndTime();
            Long ttl = endTime - now;

            if (now >= startTime && now <= endTime) {
                //3?????????????????????id????????????
                String randomCode = redisTo.getRandomCode();
                String seckillSkuId = redisTo.getSeckillSku().getPromotionSessionId() + "_" + redisTo.getSeckillSku().getSkuId();
                if (randomCode.equals(key) && seckillSkuId.equals(seckillId)) {
                    //4?????????????????????
                    Integer seckillLimit = redisTo.getSeckillSku().getSeckillLimit();
                    if (num <= seckillLimit) {
                        //5??????????????????????????????????????????????????????????????????setnx??????????????????
                        // key->seckill:member:userId_promotionSessionId_skuId, value -> ???????????????num,???????????????????????????????????????-????????????
                        MemberRespVo respVo = LoginInterceptor.loginUser.get();
                        String setNxKey = respVo.getId() + "_" + seckillSkuId;
                        Boolean hasNotBought = redisTemplate.opsForValue().setIfAbsent(SeckillConstant.SECKILL_MEMBER_PREFIX + setNxKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (hasNotBought) {
                            //6????????????????????????????????????
                            RSemaphore semaphore = redissonClient.getSemaphore(SeckillConstant.SKU_STOCK_SEMAPHORE + randomCode);
                            boolean hasStock = semaphore.tryAcquire(num);
                            if (hasStock) {
                                //7???????????????,????????????,???mq??????
                                String orderSn = IdWorker.getTimeId();
                                SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                seckillOrderTo.setOrderSn(orderSn);
                                seckillOrderTo.setSeckillPrice(redisTo.getSeckillSku().getSeckillPrice());
                                seckillOrderTo.setMemberId(respVo.getId());
                                seckillOrderTo.setNum(num);
                                seckillOrderTo.setSkuId(redisTo.getSeckillSku().getSkuId());
                                seckillOrderTo.setPromotionSessionId(redisTo.getSeckillSku().getPromotionSessionId());
                                //???mq?????????
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order",seckillOrderTo);
                                long s2 = System.currentTimeMillis();
                                log.info("???????????????{}ms", (s2 - s1));

                                //8???????????????????????????,?????????????????????,??????????????????,????????????
                                return orderSn;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * ????????????sku???????????????
     *
     * @param skuId
     * @return
     */
    @Override
    public SecKillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        //1?????????redis?????????????????????????????????
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SeckillConstant.SECKILL_SKUS_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            //2???????????????????????????????????????sku?????????????????????
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SecKillSkuRedisTo to = JSON.parseObject(json, SecKillSkuRedisTo.class);
                    //3?????????????????????????????????sku?????????????????????????????????????????????????????????null?????????????????????????????????????????????????????????
                    Long now = new Date().getTime();
                    if (now < to.getStartTime() || now > to.getEndTime()) {
                        to.setRandomCode(null);
                    }
                    return to;
                }
            }
        }

        return null;
    }


    /**
     * ???????????????????????????????????????????????????
     *
     * blockHandler ??????????????????????????????/??????/????????????????????????????????? fallback ???????????????????????????????????????
     * @return
     */
    @SentinelResource(value = "getCurrentSeckillSkusResource",blockHandler = "blockHandler")
    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        try(Entry entry = SphU.entry("getCurrentSeckillSkus")){
            //1?????????????????????????????????????????????
            Long now = new Date().getTime();
            Set<String> keys = redisTemplate.keys(SeckillConstant.SECKILL_SESSIONS_PREFIX + "*");
            for (String key : keys) {
                //seckill:sessions:1582250400000_1582254000000
                String replace = key.replace(SeckillConstant.SECKILL_SESSIONS_PREFIX, "");
                String[] s = replace.split("_");
                Long startTime = Long.parseLong(s[0]);
                Long endTime = Long.parseLong(s[1]);
                if (now >= startTime && now <= endTime) {
                    //2??????????????????????????????????????????????????????
                    //2.1??????seckill:session???????????????list????????????,[promotionSessionId_skuId]
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    //2.2??????seckill:skus???????????????????????????
                    BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SeckillConstant.SECKILL_SKUS_PREFIX);
                    List<String> list = ops.multiGet(range);
                    if (list != null) {
                        List<SecKillSkuRedisTo> collect = list.stream().map(item -> {
                            SecKillSkuRedisTo secKillSkuRedisTo = JSON.parseObject(item, SecKillSkuRedisTo.class);
                            return secKillSkuRedisTo;
                        }).collect(Collectors.toList());
                        //?????????????????????????????????????????????????????????????????????
                        return collect;
                    }
                }
            }
        }catch (BlockException e){
            log.error("???????????????,{}",e.getMessage());
        }

        return null;
    }

    /**
     * blockHandler ??????????????????????????????????????????????????????????????????????????? fallback ???????????????????????????????????????
     * @param e
     * @return
     */
    public List<SecKillSkuRedisTo> blockHandler(BlockException e){
        log.error("getCurrentSeckillSkusResource????????????..");
        return null;
    }


    /**
     * ???????????????????????????????????????
     * TODO ???????????????????????????????????????????????????????????????
     */
    @Override
    public void upSeckillSkuLatest3Days() {
        //1???????????????????????????????????????????????????
        R r = couponFeignService.getSeckillSessionLatest3Days();
        if (r.getCode() != 0) {
            throw new RRException("????????????coupon???getSeckillSessionLatest3Days??????");
        }
        List<SeckillSessionWithSkusTo> sessions = r.getData("data", new TypeReference<List<SeckillSessionWithSkusTo>>() {
        });
        if (sessions != null) {
            //2???????????????
            log.info("????????????3??????????????????...");
            //2.1?????????????????????
            saveRedisSeckillSessions(sessions);
            //2.2?????????????????????
            saveRedisSeckillSkus(sessions);

        }
    }


    /**
     * ?????????????????????value???list??????: key -> seckill:sessions:startTime_endTime  value -> [promotionSessionId_skuId]
     *
     * @param sessions
     */
    private void saveRedisSeckillSessions(List<SeckillSessionWithSkusTo> sessions) {
        sessions.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime(); //??????redis??????Long??????
            Long endTime = session.getEndTime().getTime();
            //1???key -> seckill:sessions:startTime_endTime
            String key = SeckillConstant.SECKILL_SESSIONS_PREFIX + startTime + "_" + endTime;
            //???????????????
            if (!redisTemplate.hasKey(key)) {
                //2???value -> [promotionSessionId_skuId]
                List<String> value = session.getRelationSkus().stream().map(relationSku -> {
                    return relationSku.getPromotionSessionId() + "_" + relationSku.getSkuId();
                }).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, value);
            }

        });
    }

    /**
     * ?????????????????????value???hash??????: key -> seckill:skus: value -> (key->promotionSessionId_skuId ,value->SecKillSkuRedisTo)
     *
     * @param sessions
     */
    private void saveRedisSeckillSkus(List<SeckillSessionWithSkusTo> sessions) {
        sessions.stream().forEach(session -> {
            //1?????????hash??????
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SeckillConstant.SECKILL_SKUS_PREFIX);
            session.getRelationSkus().stream().forEach(relationSku -> {
                //????????????????????????????????????
                String token = UUID.randomUUID().toString().replace("-", "");

                //???????????????
                if (!hashOps.hasKey(relationSku.getPromotionSessionId() + "_" + relationSku.getSkuId())) {
                    //2?????????redisTo
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();

                    //2.1?????????seckillSkuTo
                    redisTo.setSeckillSku(relationSku);

                    //2.2?????????skuInfoTo
                    R r = productFeignService.getSkuInfo(relationSku.getSkuId());
                    if (r.getCode() != 0) {
                        throw new RRException("????????????product???getSkuInfo??????");
                    }
                    SkuInfoTo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoTo>() {
                    });
                    redisTo.setSkuInfo(skuInfo);

                    //2.3?????????????????????????????????????????????????????????
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    //2.4????????????????????????????????????????????????????????????????????????
                    redisTo.setRandomCode(token);

                    //3??????redisTo??????json??????redis???
                    String jsonString = JSON.toJSONString(redisTo);
                    String key = relationSku.getPromotionSessionId() + "_" + relationSku.getSkuId();
                    hashOps.put(key, jsonString);

                    //4??????????????????????????????????????????????????????????????? ??????????????????
                    RSemaphore semaphore = redissonClient.getSemaphore(SeckillConstant.SKU_STOCK_SEMAPHORE + token);
                    //????????????????????????????????????????????????
                    semaphore.trySetPermits(relationSku.getSeckillCount());

                }

            });
        });
    }


}
