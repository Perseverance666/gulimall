package com.example.gulimall.seckill.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Date: 2022/11/4 19:10
 */

@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    @GetMapping("/coupon/seckillsession/seckillSessionLatest3Days")
    R getSeckillSessionLatest3Days();

}
