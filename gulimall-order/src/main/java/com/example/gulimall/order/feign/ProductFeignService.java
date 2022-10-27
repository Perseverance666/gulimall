package com.example.gulimall.order.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @Date: 2022/10/27 18:28
 */

@FeignClient("gulimall-product")
public interface ProductFeignService {

    @GetMapping("/product/spuinfo/{skuId}")
    R getSpuInfoBySkuId(@PathVariable("skuId") Long skuId);
}
