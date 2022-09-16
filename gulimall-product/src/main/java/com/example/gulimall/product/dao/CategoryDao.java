package com.example.gulimall.product.dao;

import com.example.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author liu
 * @email liu@gmail.com
 * @date 2022-09-16 13:25:00
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
