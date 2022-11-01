package com.example.gulimall.order.vo;

import lombok.Data;

/**
 * @Date: 2022/11/1 16:49
 */
@Data
public class PayVo {
    private String out_trade_no; // 商户订单号 必填
    private String subject; // 订单标题 必填
    private String total_amount;  // 付款金额 必填
    private String body; // 商品描述 可空
}
