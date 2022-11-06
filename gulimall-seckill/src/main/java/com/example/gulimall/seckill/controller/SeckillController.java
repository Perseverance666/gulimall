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
