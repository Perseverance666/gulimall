package com.example.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @Date: 2022/10/28 14:35
 *
 * 记录指定sku在哪些仓库中有库存，以及有总共有多少个
 */
@Data
public class SkuWareHasStockVo {

    private Long skuId;
    private Integer num;
    private List<Long> wareId;
}
