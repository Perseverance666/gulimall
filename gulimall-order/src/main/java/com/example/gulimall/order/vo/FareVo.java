package com.example.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Date: 2022/10/26 17:33
 * 封装 指定地址运费的vo
 */
@Data
public class FareVo {
    private MemberAddressVo address;
    private BigDecimal fare;
}
