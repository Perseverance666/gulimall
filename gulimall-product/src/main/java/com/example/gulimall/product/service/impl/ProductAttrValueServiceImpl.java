package com.example.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.ProductAttrValueDao;
import com.example.gulimall.product.entity.ProductAttrValueEntity;
import com.example.gulimall.product.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取spu规格
     * 商品系统，商品维护，spu管理，点击规格按钮回显功能
     * @param spuId
     * @return
     */
    @Override
    public List<ProductAttrValueEntity> baseAttrListforspu(Long spuId) {
        LambdaQueryWrapper<ProductAttrValueEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(spuId != null,ProductAttrValueEntity::getSpuId,spuId);
        List<ProductAttrValueEntity> entities = this.baseMapper.selectList(lqw);
        return entities;
    }

    /**
     * 修改商品规格
     * 商品系统，商品维护，spu管理，点击规格按钮，进行修改功能
     * @param spuId
     * @param entities
     * @return
     */
    @Transactional
    @Override
    public void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities) {
        //不判断更新，还是新增了。直接上传指定spuId的原来的数据
        LambdaQueryWrapper<ProductAttrValueEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(spuId != null,ProductAttrValueEntity::getSpuId,spuId);
        this.baseMapper.delete(lqw);

        //删除之后，直接插入新数据
        List<ProductAttrValueEntity> collect = entities.stream().map((entity) -> {
            entity.setSpuId(spuId);
            return entity;
        }).collect(Collectors.toList());

        this.saveBatch(collect);
    }

}