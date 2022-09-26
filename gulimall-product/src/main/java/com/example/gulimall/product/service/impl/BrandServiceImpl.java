package com.example.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gulimall.product.service.CategoryBrandRelationService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.BrandDao;
import com.example.gulimall.product.entity.BrandEntity;
import com.example.gulimall.product.service.BrandService;
import org.springframework.transaction.annotation.Transactional;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    /**
     * 商品系统，品牌管理，列表展示
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        LambdaQueryWrapper<BrandEntity> lqw = new LambdaQueryWrapper<>();
        if(!StringUtils.isEmpty(key)){
            lqw.eq(BrandEntity::getBrandId,key).or().like(BrandEntity::getName,key);
        }

        IPage<BrandEntity> page = this.page(new Query<BrandEntity>().getPage(params),lqw);

        return new PageUtils(page);
    }

    /**
     * 商品系统，品牌管理，修改功能
     * 更新关联的其他表的信息，保证冗余字段的数据一致
     * @param brand
     */
    @Transactional
    @Override
    public void updateDetail(BrandEntity brand) {
        //先更新brand表
        this.updateById(brand);

        if(!StringUtils.isEmpty(brand.getName())){
            //同步更新其他关联表中数据
            categoryBrandRelationService.updateBrand(brand.getBrandId(),brand.getName());

            //TODO  更新其他关联
        }


    }

}