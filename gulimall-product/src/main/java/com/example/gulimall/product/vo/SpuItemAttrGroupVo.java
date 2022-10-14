package com.example.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @Date: 2022/10/14 20:30
 * 商品详情，spu的规格参数信息
 */

@Data
public class SpuItemAttrGroupVo {

    private String groupName;
    private List<Attr> attrs;
}

