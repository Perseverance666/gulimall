package com.example.common.to.mq;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Date: 2022/11/6 19:25
 * 秒杀功能中，秒杀成功后，给mq发消息的to，再去慢慢完成订单相关业务
 */

@Data
public class SeckillOrderTo {
    private String orderSn; //订单号
    private Long promotionSessionId;  //活动场次id
    private Long skuId;  //商品id
    private BigDecimal seckillPrice; //秒杀价格
    private Integer num; //购买数量
    private Long memberId;//会员id；
}
