package com.example.gulimall.seckill.controller;

import com.example.common.utils.R;
import com.example.gulimall.seckill.service.SeckillService;
import com.example.gulimall.seckill.to.SecKillSkuRedisTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @Date: 2022/11/5 20:05
 *
 * 秒杀(高并发)系统关注的问题:
 *    1)、服务单一职责 + 独立部署：秒杀服务即使自己扛不住压力，挂掉，也不能影响其他服务
 *    2)、秒杀链接加密：给秒杀商品设置随机码，到秒杀时间才对外暴露，拿着随机码才能进行秒杀。防止恶意攻击，防止链接暴露
 *    3)、库存预热 + 快速扣减：定时任务上架后，秒杀商品的库存以信号量形式放入redis，做到原子减量。
 *                          秒杀读多写少，无需每次实时校验库存。库存预热，放入redis中。信号量控制进来秒杀的请求
 *    4)、动静分离：nginx做好动静分离，保证秒杀和商品详情页的动态请求才打到后端的服务集群。使用CDN网络，分担本集群压力
 *    5)、恶意请求拦截：识别非法攻击请求并进行拦截，网关层解决
 *    6)、流量错峰：使用各种手段将流量分担到更大宽度的时间点。比如验证码、加入购物车
 *    7)、限流、熔断、降级：前端限流+后端限流、限制次数、限制总量，快速失败降级运行，熔断隔离防止雪崩
 *    8)、队列削峰：所有秒杀成功的请求进入队列，慢慢创建订单，扣减库存即可
 */

@Slf4j
@Controller
public class SeckillController {
    @Autowired
    private SeckillService seckillService;

    /**
     * 秒杀功能
     * @param seckillId
     * @param key
     * @param num
     * @return
     */
    @GetMapping("/seckill")
    public String seckill(@RequestParam("seckillId") String seckillId,
                          @RequestParam("key") String key,
                          @RequestParam("num") Integer num,
                          Model model) {
        String orderSn = seckillService.seckill(seckillId,key,num);
        model.addAttribute("orderSn",orderSn);
        return "success";
    }

    /**
     * 获取当前时间可以参与的秒杀商品信息
     * @return
     */
    @ResponseBody
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
    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId){
        SecKillSkuRedisTo secKillSkuRedisTo = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().put("data",secKillSkuRedisTo);
    }
}
