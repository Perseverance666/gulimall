package com.example.common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Date: 2022/9/25 19:01
 */

@Data
public class SpuBoundTo {
    private Long spuId;

    private BigDecimal buyBounds;

    private BigDecimal growBounds;
}
