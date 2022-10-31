package com.example.gulimall.ware.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @Date: 2022/10/31 17:00
 */

@FeignClient("gulimall-order")
public interface OrderFeignService {


    @GetMapping("/order/order/orderSn/{orderSn}")
    R getOrderByOrderSn(@PathVariable("orderSn") String orderSn);
}
