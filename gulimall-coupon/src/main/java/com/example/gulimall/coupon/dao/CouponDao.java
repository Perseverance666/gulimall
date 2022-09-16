package com.example.gulimall.coupon.dao;

import com.example.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author ll
 * @email ll@gmail.com
 * @date 2022-08-13 13:51:14
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
