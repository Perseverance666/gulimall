package com.example.gulimall.ware.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @Date: 2022/10/26 17:18
 */

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @GetMapping("/member/memberreceiveaddress/info/{id}")
    R getAddrinfo(@PathVariable("id") Long id);
}
