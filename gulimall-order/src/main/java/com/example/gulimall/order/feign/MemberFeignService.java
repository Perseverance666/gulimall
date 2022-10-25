package com.example.gulimall.order.feign;

import com.example.gulimall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @Date: 2022/10/25 17:09
 */

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @GetMapping("/member/memberreceiveaddress/addresses/{memberId}")
    List<MemberAddressVo> getAddresses(@PathVariable Long memberId);
}
