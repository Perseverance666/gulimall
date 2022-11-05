package com.example.gulimall.product.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Date: 2022/11/4 19:58
 * 秒杀商品的vo，和coupon中的SeckillSkuRelationEntity属性值一样
 */

@Data
public class SeckillSkuTo {

    private Long id;
    /**
     * 活动id
     */
    private Long promotionId;

    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private Integer seckillCount;
    /**
     * 每人限购数量
     */
    private Integer seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;
}
