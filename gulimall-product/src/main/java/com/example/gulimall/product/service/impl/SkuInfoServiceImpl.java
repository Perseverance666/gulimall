package com.example.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.exception.RRException;
import com.example.common.utils.R;
import com.example.gulimall.product.entity.SkuImagesEntity;
import com.example.gulimall.product.entity.SpuInfoDescEntity;
import com.example.gulimall.product.entity.SpuInfoEntity;
import com.example.gulimall.product.feign.SeckillFeignService;
import com.example.gulimall.product.service.*;
import com.example.gulimall.product.to.SecKillSkuRedisTo;
import com.example.gulimall.product.vo.SkuItemSaleAttrVo;
import com.example.gulimall.product.vo.SkuItemVo;
import com.example.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

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
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    private SeckillFeignService seckillFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * sku??????
     * ?????????????????????????????????????????? ????????????
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
                if(bigDecimal.compareTo(new BigDecimal("0")) == 1){     //?????????max>0???????????????lqw???
                    lqw.le(SkuInfoEntity::getPrice,max);        //price <= max
                }
            }catch (Exception e){

            }
        }

        IPage<SkuInfoEntity> page = this.page(new Query<SkuInfoEntity>().getPage(params),lqw);
        return new PageUtils(page);
    }

    /**
     * ??????spuId????????????sku??????
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
     * ??????????????????????????????sku?????????????????????????????????
     * ?????????????????????,??????CompletableFuture
     * @param skuId
     * @return
     */
    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();

        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            //1???sku??????????????????  pms_sku_info
            SkuInfoEntity info = this.getById(skuId);
            skuItemVo.setInfo(info);
            return info;
        }, executor);

        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            //2???sku???????????????  pms_sku_images
            LambdaQueryWrapper<SkuImagesEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(SkuImagesEntity::getSkuId, skuId);
            List<SkuImagesEntity> images = skuImagesService.list(lqw);
            skuItemVo.setImages(images);
        }, executor);

        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            //3?????????spu?????????????????????
            List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, executor);

        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            //4?????????spu?????????
            SpuInfoDescEntity desp = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesp(desp);
        }, executor);

        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            //5?????????spu?????????????????????
            List<SpuItemAttrGroupVo> spuItemAttrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId());
            skuItemVo.setGroupAttrs(spuItemAttrGroupVos);
        }, executor);

        //6???????????????sku????????????????????????
        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            R r = seckillFeignService.getSkuSeckillInfo(skuId);
            if (r.getCode() != 0) {
                throw new RRException("????????????seckill???getSkuSeckillInfo??????");
            }
            SecKillSkuRedisTo seckillInfo = r.getData("data", new TypeReference<SecKillSkuRedisTo>() {});
            skuItemVo.setSeckillInfo(seckillInfo);
        },executor);


        //???????????????????????????????????????skuItemVo
        CompletableFuture.anyOf(imagesFuture,saleAttrFuture,descFuture,baseAttrFuture,seckillFuture).get();
        return skuItemVo;
    }


}