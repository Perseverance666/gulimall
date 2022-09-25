package com.example.common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Date: 2022/9/25 19:19
 */

@Data
public class SkuReductionTo {
    private Long skuId;
    private int fullCount;
    private BigDecimal discount;
    private int countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private int priceStatus;
    private List<MemberPrice> memberPrice;
}
