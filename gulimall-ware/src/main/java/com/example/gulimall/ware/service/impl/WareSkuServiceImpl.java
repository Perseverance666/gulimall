package com.example.gulimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.utils.R;
import com.example.gulimall.ware.feign.ProductFeignService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.ware.dao.WareSkuDao;
import com.example.gulimall.ware.entity.WareSkuEntity;
import com.example.gulimall.ware.service.WareSkuService;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private ProductFeignService productFeignService;
    /**
     * 查询商品库存
     * 库存系统，商品库存，列表展示
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<WareSkuEntity> lqw = new LambdaQueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(StringUtils.isNotEmpty(skuId)){
            lqw.eq(WareSkuEntity::getSkuId,skuId);
        }
        String wareId = (String) params.get("wareId");
        if(StringUtils.isNotEmpty(wareId)){
            lqw.eq(WareSkuEntity::getWareId,wareId);
        }

        IPage<WareSkuEntity> page = this.page(new Query<WareSkuEntity>().getPage(params),lqw);
        return new PageUtils(page);
    }

    /**
     * 完成采购
     * 模拟采购人员app实现完成采购时的加入库存功能
     * @param skuId
     * @param wareId
     * @param skuNum
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //先判断库存中是否有货
        LambdaQueryWrapper<WareSkuEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(skuId != null,WareSkuEntity::getSkuId,skuId)
                .eq(wareId != null,WareSkuEntity::getWareId,wareId);
        List<WareSkuEntity> wareSkuEntities = this.list(lqw);
        if(wareSkuEntities == null ||wareSkuEntities.size() == 0){
            //库存中没有货，新增操作
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //TODO 远程查询sku的名字，如果失败，整个事务无需回滚
            //1、自己catch异常
            //2、TODO 高级篇讲其他方法
            try{
                R info = productFeignService.info(skuId);
                Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");
                if(info.getCode() == 0){
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }

            }catch (Exception e){

            }

            this.baseMapper.insert(wareSkuEntity);
        }else {
            //库存中有货，修改操作
            this.baseMapper.addStock(skuId,wareId,skuNum);
        }

    }

}