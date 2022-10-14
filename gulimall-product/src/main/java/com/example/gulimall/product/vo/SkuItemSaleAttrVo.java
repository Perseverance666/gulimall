package com.example.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @Date: 2022/10/14 20:26
 * 商品详情，sku销售属性
 */

@Data
public class SkuItemSaleAttrVo {
    private Long attrId;
    private String attrName;
    private List<AttrValueWithSkuIdVo> attrValues;
}
