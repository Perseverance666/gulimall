package com.example.gulimall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.coupon.dao.SeckillSkuRelationDao;
import com.example.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.example.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.util.StringUtils;


@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<SeckillSkuRelationEntity> lqw = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            lqw.eq(SeckillSkuRelationEntity::getSkuId,key);
        }
        String promotionSessionId = (String) params.get("promotionSessionId");
        if(!StringUtils.isEmpty(promotionSessionId)){
            lqw.eq(SeckillSkuRelationEntity::getPromotionSessionId,promotionSessionId);
        }

        IPage<SeckillSkuRelationEntity> page = this.page(new Query<SeckillSkuRelationEntity>().getPage(params),lqw);

        return new PageUtils(page);
    }

}