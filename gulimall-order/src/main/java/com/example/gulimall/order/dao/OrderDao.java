package com.example.gulimall.order.dao;

import com.example.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author ll
 * @email ll@gmail.com
 * @date 2022-08-13 14:15:59
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
