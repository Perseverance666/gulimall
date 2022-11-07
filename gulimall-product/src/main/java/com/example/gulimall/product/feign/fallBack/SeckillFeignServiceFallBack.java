package com.example.gulimall.product.feign.fallBack;

import com.example.common.exception.BizCodeEnum;
import com.example.common.utils.R;
import com.example.gulimall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Date: 2022/11/7 21:22
 * 远程调用SeckillFeignService失败后的熔断处理
 */

@Slf4j
@Component
public class SeckillFeignServiceFallBack implements SeckillFeignService {
    @Override
    public R getSkuSeckillInfo(Long skuId) {
        log.info("熔断方法调用...getSkuSeckillInfo");
        return R.error(BizCodeEnum.TOO_MANY_REQUEST.getCode(), BizCodeEnum.TOO_MANY_REQUEST.getMsg());
    }
}
