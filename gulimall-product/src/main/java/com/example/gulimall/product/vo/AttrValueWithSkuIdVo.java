package com.example.gulimall.product.vo;

import lombok.Data;

/**
 * @Date: 2022/10/14 20:27
 * 有哪些sku包含该属性值
 */

@Data
public class AttrValueWithSkuIdVo {

    private String attrValue;
    private String skuIds;
}
