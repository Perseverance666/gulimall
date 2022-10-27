package com.example.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.constant.ProductConstant;
import com.example.common.es.SkuEsModel;
import com.example.common.to.SkuHasStockTo;
import com.example.common.to.SkuReductionTo;
import com.example.common.to.SpuBoundTo;
import com.example.common.utils.R;
import com.example.gulimall.product.entity.*;
import com.example.gulimall.product.feign.CouponFeignService;
import com.example.gulimall.product.feign.SearchFeignService;
import com.example.gulimall.product.feign.WareFeignService;
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
    private BrandService brandService;
    @Autowired
    private CategoryService categoryService;
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
    @Autowired
    private WareFeignService wareFeignService;
    @Autowired
    private SearchFeignService searchFeignService;


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
     * 商品系统，商品维护，发布商品，输入SKU信息后，点击下一步：保存商品信息
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

    /**
     * spu检索
     * 商品系统，商品维护，spu管理 页面展示
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        LambdaQueryWrapper<SpuInfoEntity> lqw = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");
        if(StringUtils.isNotEmpty(key)){
            lqw.and((wrapper) -> {
                wrapper.eq(SpuInfoEntity::getId,key).or().like(SpuInfoEntity::getSpuName,key);
            });
        }
        String status = (String) params.get("status");
        if(StringUtils.isNotEmpty(status)){
            lqw.eq(SpuInfoEntity::getPublishStatus,status);
        }
        String brandId = (String) params.get("brandId");
        if(StringUtils.isNotEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            lqw.eq(SpuInfoEntity::getBrandId,brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if(StringUtils.isNotEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            lqw.eq(SpuInfoEntity::getCatalogId,catelogId);
        }
        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(params),lqw);

        return new PageUtils(page);
    }

    /**
     * 商品上架
     * 商品系统，商品维护，spu管理，上架功能
     * @param spuId
     * @return
     */
    @Override
    public void up(Long spuId) {
        //1、根据spuId查询对应sku信息
        List<SkuInfoEntity> skuInfoEntities = skuInfoService.getSkusBySpuId(spuId);

        //2、查出当前sku的所有可以被用来检索的规格属性
        List<ProductAttrValueEntity> productAttrValueEntities = productAttrValueService.baseAttrListforspu(spuId);
        //将attrId收集出来
        List<Long> attrIds = productAttrValueEntities.stream().map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());
        //查询出可检索，即search_type=1的全部attrId
        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);
        //封装SkuEsModel中的attrs属性
        List<SkuEsModel.Attrs> attrsList = productAttrValueEntities.stream().filter((item) -> {
            return searchAttrIds.contains(item.getAttrId());
        }).map((item) -> {
            SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs);
            return attrs;
        }).collect(Collectors.toList());

        //3、封装SkuEsModel
        List<Long> skuIds = skuInfoEntities.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        //发送远程调用，查询库存系统是否有库存
        Map<Long, Boolean> stockMap = null;
        try{            //远程调用，为防止异常，使用try catch
            R skusHasStock = wareFeignService.getSkusHasStock(skuIds);
            //将结果收集成map，skuId为key，hasStock为value,后面使用方便
            TypeReference<List<SkuHasStockTo>> typeReference = new TypeReference<List<SkuHasStockTo>>() {};
            stockMap = skusHasStock.getData(typeReference).stream()
                    .collect(Collectors.toMap(SkuHasStockTo::getSkuId, SkuHasStockTo::isHasStock));
        }catch (Exception e){
            log.error("库存服务查询异常：原因{}",e);
        }

        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> esModels = skuInfoEntities.stream().map((sku) -> {
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku,esModel);
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());
            //设置库存信息
            if(finalStockMap == null){
                //若出现问题，默认设置有库存
                esModel.setHasStock(true);
            }else{
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }
            //热度评分：0
            esModel.setHotScore(0L);
            //查询品牌和分类信息
            BrandEntity brand = brandService.getById(sku.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());
            CategoryEntity category = categoryService.getById(sku.getCatalogId());
            esModel.setCatalogName(category.getName());
            //设置检索属性
            esModel.setAttrs(attrsList);

            return esModel;
        }).collect(Collectors.toList());

        //4、 将数据发送给es进行保存
        R r = searchFeignService.productStatusUp(esModels);
        if(r.getCode() == 0){
            //修改当前spu状态
            SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
            spuInfoEntity.setId(spuId);
            spuInfoEntity.setPublishStatus(ProductConstant.ProductStatusEnum.SPU_UP.getCode());
            spuInfoEntity.setUpdateTime(new Date());
            this.updateById(spuInfoEntity);
        } else {
            //远程调用失败
            //TODO 7、重复调用？接口幂等性；重试机制？xxx
            //Feign调用流程
            /**
             * 1、构造请求数据，将对象转为json；
             *      RequestTemplate template = buildTemplateFromArgs.create(argv);
             * 2、发送请求进行执行（执行成功会解码响应数据）：
             *      executeAndDecode(template);
             * 3、执行请求会有重试机制
             *      while(true){
             *          try{
             *            executeAndDecode(template);
             *          }catch(){
             *              try{retryer.continueOrPropagate(e);}catch(){throw ex;}
             *              continue;
             *          }
             *
             *      }
             */
        }

    }

    /**
     * 根据skuId查询spu信息
     * @param skuId
     * @return
     */
    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
        SkuInfoEntity skuInfo = skuInfoService.getById(skuId);
        SpuInfoEntity spuInfo = null;
        if(skuInfo != null){
            spuInfo = this.getById(skuInfo.getSpuId());
        }
        return spuInfo;
    }

}