package com.example.gulimall.coupon.service.impl;

import com.example.common.to.MemberPrice;
import com.example.common.to.SkuReductionTo;
import com.example.gulimall.coupon.entity.MemberPriceEntity;
import com.example.gulimall.coupon.entity.SkuLadderEntity;
import com.example.gulimall.coupon.service.MemberPriceService;
import com.example.gulimall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.coupon.dao.SkuFullReductionDao;
import com.example.gulimall.coupon.entity.SkuFullReductionEntity;
import com.example.gulimall.coupon.service.SkuFullReductionService;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {
    @Autowired
    private SkuLadderService skuLadderService;
    @Autowired
    private MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存sku的优惠、满减等信息 (跨库)gulimall_sms -> sms_sku_ladder \ sms_sku_full_reduction \ sms_member_price
     * @param skuReductionTo
     */
    @Override
    public void saveSkuReduction(SkuReductionTo skuReductionTo) {
        //1、sms_sku_ladder
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuReductionTo,skuLadderEntity);
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
        //如果有打折，保存到sms_sku_ladder
        if(skuReductionTo.getFullCount() > 0){
            skuLadderService.save(skuLadderEntity);
        }


        //2、sms_sku_full_reduction
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuReductionTo,skuFullReductionEntity);
        //如果有满减，保存到ssms_sku_full_reduction
        if(skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1){
            this.save(skuFullReductionEntity);
        }


        //3、 sms_member_price
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();
        List<MemberPriceEntity> collect = memberPrice.stream().map((item) -> {
            MemberPriceEntity entity = new MemberPriceEntity();
            entity.setSkuId(skuReductionTo.getSkuId());
            entity.setMemberLevelId(item.getId());
            entity.setMemberLevelName(item.getName());
            entity.setMemberPrice(item.getPrice());
            entity.setAddOther(1);
            return entity;
        }).filter((item) -> {
            //只有设置了会员价格才保存到sms_member_price中
            return item.getMemberPrice().compareTo(new BigDecimal("0")) == 1;
        }).collect(Collectors.toList());
        memberPriceService.saveBatch(collect);

    }

}