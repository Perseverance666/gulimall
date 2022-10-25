package com.example.gulimall.order.feign;

import com.example.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * @Date: 2022/10/25 17:41
 */

@FeignClient("gulimall-cart")
public interface CartFeignService {

    @GetMapping("/currentUserCheckedCartItems")
    List<OrderItemVo> getCurrentUserCheckedCartItems();
}
