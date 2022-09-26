package com.example.gulimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
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

}