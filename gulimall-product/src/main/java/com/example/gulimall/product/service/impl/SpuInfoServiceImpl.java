package com.example.gulimall.product.service.impl;

import com.example.common.to.SkuReductionTo;
import com.example.common.to.SpuBoundTo;
import com.example.common.utils.R;
import com.example.gulimall.product.entity.*;
import com.example.gulimall.product.feign.CouponFeignService;
import com.example.gulimall.product.service.*;
import com.example.gulimall.product.vo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private SpuImagesService spuImagesService;
    @Autowired
    private AttrService attrService;
    @Autowired
    private ProductAttrValueService productAttrValueService;
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private CouponFeignService couponFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * TODO 高级部分再来完善
     * 新增商品
     * 商品维护，发布商品，输入SKU信息后，点击下一步：保存商品信息
     * @param vo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        //1、保存spu的基本信息 pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.baseMapper.insert(spuInfoEntity);

        //2、保存spu的描述图片 pms_spu_info_desc
        Long spuId = spuInfoEntity.getId();
        List<String> decript = vo.getDecript();
        if(decript != null && decript.size() > 0){
            SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
            descEntity.setSpuId(spuId);
            descEntity.setDecript(String.join(",",decript));
            spuInfoDescService.save(descEntity);
        }

        //3、保存spu的图片集 pms_spu_images
        List<String> images = vo.getImages();
        if(images != null && images.size() > 0){
            List<SpuImagesEntity> spuImagesEntities = images.stream().map((image) -> {
                SpuImagesEntity spuImagesEntity = new SpuImagesEntity();
                spuImagesEntity.setSpuId(spuId);
                spuImagesEntity.setImgUrl(image);
                return spuImagesEntity;
            }).collect(Collectors.toList());
            spuImagesService.saveBatch(spuImagesEntities);
        }

        //4、保存spu的规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        if(baseAttrs != null && baseAttrs.size() > 0){
            List<ProductAttrValueEntity> productAttrValueEntities = baseAttrs.stream().map((attr) -> {
                ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
                valueEntity.setSpuId(spuId);
                valueEntity.setAttrId(attr.getAttrId());

                AttrEntity byId = attrService.getById(attr.getAttrId());
                valueEntity.setAttrName(byId.getAttrName());

                valueEntity.setAttrValue(attr.getAttrValues());
                valueEntity.setQuickShow(attr.getShowDesc());

                return valueEntity;
            }).collect(Collectors.toList());
            productAttrValueService.saveBatch(productAttrValueEntities);
        }

        //5、保存spu的积分信息 (跨库)gulimall_sms -> sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds,spuBoundTo);
        spuBoundTo.setSpuId(spuId);
        R r2 = couponFeignService.saveSpuBounds(spuBoundTo);
        if(r2.getCode() != 0){
            log.error("远程保存spu积分信息失败！");
        }

        //6、保存当前spu对应的所有sku信息
        List<Skus> skus = vo.getSkus();
        if(skus != null && skus.size() > 0){
            //由于后面一直用到skuId，故不用stream流收集起来再保存了，foreach遍历过程中一个一个保存到pms_sku_info表中
            skus.forEach((sku) -> {
                //6.1、保存sku的基本信息 pms_sku_info
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku,skuInfoEntity);
                skuInfoEntity.setSpuId(spuId);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);

                List<Images> imgs = sku.getImages();
                for (Images image : imgs){
                    if(image.getDefaultImg() == 1){
                        skuInfoEntity.setSkuDefaultImg(image.getImgUrl());
                        break;
                    }
                }
                skuInfoService.save(skuInfoEntity);
                //保存基本信息到pms_sku_info表中后，获取skuId，接下来一直要用
                Long skuId = skuInfoEntity.getSkuId();
                //6.2、保存sku的图片信息 pms_sku_images
                List<SkuImagesEntity> skuImagesEntities = sku.getImages().stream().map((img) -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter((entity) -> {
                    //返回true,即有照片，就保存到表中，false就不保存
                    return StringUtils.isNotEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(skuImagesEntities);

                //6.3、sku的销售属性信息 pms_sku_sale_attr_value
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = sku.getAttr().stream().map((saleAttr) -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(saleAttr, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                //6.4、保存sku的优惠、满减等信息 (跨库)gulimall_sms -> sms_sku_ladder \ sms_sku_full_reduction \ sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku,skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                //有满减优惠，则保存到表中，不保存满0减0这样无用的数据
                if(skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1){
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if(r1.getCode() != 0){
                        log.error("远程保存sku的优惠、满减等信息失败！");
                    }
                }


            });
        }


    }

}