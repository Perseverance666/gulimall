package com.example.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gulimall.product.entity.SkuImagesEntity;
import com.example.gulimall.product.entity.SpuInfoDescEntity;
import com.example.gulimall.product.entity.SpuInfoEntity;
import com.example.gulimall.product.service.*;
import com.example.gulimall.product.vo.SkuItemVo;
import com.example.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.SkuInfoDao;
import com.example.gulimall.product.entity.SkuInfoEntity;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private AttrGroupService attrGroupService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * sku检索
     * 商品系统，商品维护，商品管理 列表展示
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        LambdaQueryWrapper<SkuInfoEntity> lqw = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");
        if(StringUtils.isNotEmpty(key)){
            lqw.and((wrapper) -> {
                wrapper.eq(SkuInfoEntity::getSkuId,key).or().like(SkuInfoEntity::getSkuName,key);
            });
        }
        String brandId = (String) params.get("brandId");
        if(StringUtils.isNotEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            lqw.eq(SkuInfoEntity::getBrandId,brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if(StringUtils.isNotEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            lqw.eq(SkuInfoEntity::getCatalogId,catelogId);
        }
        String min = (String) params.get("min");
        if(StringUtils.isNotEmpty(min)){
            lqw.ge(SkuInfoEntity::getPrice,min);       //price >= min
        }
        String max = (String) params.get("max");
        if(StringUtils.isNotEmpty(max)){
            try {
                BigDecimal bigDecimal = new BigDecimal(max);
                if(bigDecimal.compareTo(new BigDecimal("0")) == 1){     //只有当max>0时，才加入lqw中
                    lqw.le(SkuInfoEntity::getPrice,max);        //price <= max
                }
            }catch (Exception e){

            }
        }

        IPage<SkuInfoEntity> page = this.page(new Query<SkuInfoEntity>().getPage(params),lqw);
        return new PageUtils(page);
    }

    /**
     * 根据spuId查询对应sku信息
     * @param spuId
     * @return
     */
    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        LambdaQueryWrapper<SkuInfoEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(spuId != null,SkuInfoEntity::getSpuId,spuId);
        List<SkuInfoEntity> skuInfoEntities = this.list(lqw);
        return skuInfoEntities;
    }

    /**
     * 页面商品详情中将指定sku相关的所有信息进行展示
     * 要进行异步调用,使用CompletableFuture
     * @param skuId
     * @return
     */
    @Override
    public SkuItemVo item(Long skuId) {
        SkuItemVo skuItemVo = new SkuItemVo();
        //1、sku基本信息获取  pms_sku_info
        SkuInfoEntity info = this.getById(skuId);
        skuItemVo.setInfo(info);
        Long spuId = info.getSpuId();
        Long catalogId = info.getCatalogId();

        //2、sku的图片信息  pms_sku_images
        LambdaQueryWrapper<SkuImagesEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SkuImagesEntity::getSkuId,skuId);
        List<SkuImagesEntity> images = skuImagesService.list(lqw);
        skuItemVo.setImages(images);
        //3、获取spu的销售属性组合



        //4、获取spu的介绍
        SpuInfoDescEntity desp = spuInfoDescService.getById(spuId);
        skuItemVo.setDesp(desp);

        //5、获取spu的规格参数信息
        List<SpuItemAttrGroupVo> groupAttrs = attrGroupService.getAttrGroupWithAttrsBySpuId(spuId,catalogId);
        skuItemVo.setGroupAttrs(groupAttrs);

        return skuItemVo;
    }

}