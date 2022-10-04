package com.example.common.to;

import lombok.Data;

/**
 * @Date: 2022/10/4 16:12
 */

@Data
public class SkuHasStockTo {
    private Long skuId;
    private boolean hasStock;
}
