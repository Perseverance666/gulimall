package com.example.gulimall.product.vo;

import com.example.gulimall.product.entity.SkuImagesEntity;
import com.example.gulimall.product.entity.SkuInfoEntity;
import com.example.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

/**
 * @Date: 2022/10/14 20:23
 * 商品详情，页面展示的sku所有属性
 */

@Data
public class SkuItemVo {
    //1、sku基本信息获取  pms_sku_info
    private SkuInfoEntity info;

    private boolean hasStock = true;

    //2、sku的图片信息  pms_sku_images
    private List<SkuImagesEntity> images;

    //3、获取spu的销售属性组合。
    private List<SkuItemSaleAttrVo> saleAttr;

    //4、获取spu的介绍
    private SpuInfoDescEntity desp;

    //5、获取spu的规格参数信息。
    private List<SpuItemAttrGroupVo> groupAttrs;
}
