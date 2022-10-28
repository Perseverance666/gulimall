package com.example.gulimall.order.vo;

import lombok.Data;

/**
 * @Date: 2022/10/26 16:24
 *
 * 封装 指定skuId是否有库存
 */
@Data
public class SkuStockVo {
    private Long skuId;
    private Boolean hasStock;
}
