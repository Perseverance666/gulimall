package com.example.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.common.constant.ProductConstant;
import com.example.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.example.gulimall.product.dao.AttrGroupDao;
import com.example.gulimall.product.dao.CategoryDao;
import com.example.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.example.gulimall.product.entity.AttrGroupEntity;
import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.CategoryService;
import com.example.gulimall.product.vo.AttrRespVo;
import com.example.gulimall.product.vo.AttrVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.AttrDao;
import com.example.gulimall.product.entity.AttrEntity;
import com.example.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private AttrGroupDao attrGroupDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存属性【规格参数，销售属性】
     * 商品系统，平台属性，规格参数，销售属性的新增
     * @param attr
     */
    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        //1、保存基本属性
        this.save(attrEntity);
        //2、如果属性是基本属性，保存关联关系
        if(attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId() != null){
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrId(attrEntity.getAttrId());
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            attrAttrgroupRelationDao.insert(relationEntity);
        }

    }

    /**
     * 商品系统，获取分类规格参数,attrType=base 规格参数右侧列表展示
     * 商品系统，获取分类销售属性,attrType=sale 销售属性右侧列表展示
     * @param params
     * @param catelogId
     * @param attrType
     * @return
     */
    @Override
    public PageUtils queryAttrPage(Map<String, Object> params, Long catelogId, String attrType) {
        LambdaQueryWrapper<AttrEntity> lqw = new LambdaQueryWrapper<>();
        //请求路径中attrType为base时，查基本属性，此时valueType=1；请求路径中attrType为sale时，查销售属性，此时valueType=0
        lqw.eq(attrType != null, AttrEntity::getAttrType,
                "base".equalsIgnoreCase(attrType) ? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            lqw.and((wrapper) -> {
                wrapper.eq(AttrEntity::getAttrId, key).or().like(AttrEntity::getAttrName, key);
            });
        }

        //catelogId == 0时，查询全部
        if (catelogId != 0) {
            lqw.eq(catelogId != null, AttrEntity::getCatelogId, catelogId);

        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), lqw);
        PageUtils pageUtils = new PageUtils(page);

        List<AttrRespVo> respVos = page.getRecords().stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            //设置 所属分类
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }

            //设置 所属分组
            //如果是基本属性，就查询分组信息；如果是销售属性，就不用查分组信息
            if ("base".equalsIgnoreCase(attrType)) {
                LambdaQueryWrapper<AttrAttrgroupRelationEntity> lqw2 = new LambdaQueryWrapper<>();
                lqw2.eq(AttrAttrgroupRelationEntity::getAttrId, attrEntity.getAttrId());
                AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(lqw2);
                if (relationEntity != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                    if (attrGroupEntity != null) {
                        attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                    }
                }
            }

            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(respVos);
        return pageUtils;
    }

    /**
     * 查询属性详情
     * 商品系统，平台属性，规格参数和销售属性的修改功能回显
     * @param attrId
     * @return
     */
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrEntity attrEntity = this.getById(attrId);
        AttrRespVo attrRespVo = new AttrRespVo();
        BeanUtils.copyProperties(attrEntity, attrRespVo);

        //若当前属性是基本属性，设置分组信息
        if(attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            //1、设置分组信息
            LambdaQueryWrapper<AttrAttrgroupRelationEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(AttrAttrgroupRelationEntity::getAttrId, attrId);
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(lqw);
            if (relationEntity != null) {
                attrRespVo.setAttrGroupId(relationEntity.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                if (attrGroupEntity != null) {
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }
        //2、设置分类信息
        Long[] catelogPath = categoryService.findCatelogPath(attrEntity.getCatelogId());
        attrRespVo.setCatelogPath(catelogPath);
        CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
        if (categoryEntity != null) {
            attrRespVo.setCatelogName(categoryEntity.getName());
        }

        return attrRespVo;
    }

    /**
     * 修改属性
     * 商品系统，平台属性，规格参数，销售属性的修改功能
     * @param attr
     */
    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);

        //若当前属性是基本属性，修改分组信息
        if(attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            //修改分组关联
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrId(attr.getAttrId());
            relationEntity.setAttrGroupId(attr.getAttrGroupId());

            //先查询关联表中是否有分组信息
            LambdaQueryWrapper<AttrAttrgroupRelationEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(AttrAttrgroupRelationEntity::getAttrId, attr.getAttrId());
            Integer count = attrAttrgroupRelationDao.selectCount(lqw);

            if (count > 0) {              //表中有数据，更新
                LambdaUpdateWrapper<AttrAttrgroupRelationEntity> luw = new LambdaUpdateWrapper<>();
                luw.eq(AttrAttrgroupRelationEntity::getAttrId, attr.getAttrId());
                attrAttrgroupRelationDao.update(relationEntity, luw);
            } else {                      //表中无数据，新增
                attrAttrgroupRelationDao.insert(relationEntity);
            }
        }

    }

    /**
     * 获取属性分组的关联的所有属性
     * 商品系统，平台属性，属性分组，关联展示
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        LambdaQueryWrapper<AttrAttrgroupRelationEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(attrgroupId != null,AttrAttrgroupRelationEntity::getAttrGroupId,attrgroupId);
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(lqw);

        List<Long> attrIds = relationEntities.stream().map((relationEntity) -> {
            return relationEntity.getAttrId();
        }).collect(Collectors.toList());

        if(attrIds == null ||attrIds.size() == 0){
            return null;
        }

        List<AttrEntity> attrEntities = this.listByIds(attrIds);
        return attrEntities;
    }

    /**
     * 获取属性分组没有关联的其他属性
     * 商品系统，平台展示，属性分组，关联，新建关联后的展示
     * 展示该分类下，展示没有被其他属性分组关联的规格参数
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        //1、当前分组只能关联自己所属分类里面的规格参数
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        //2、当前分组只能关联别的分组没有关联的规格参数
        //2.1、找到当前分类的所有分组
        LambdaQueryWrapper<AttrGroupEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(catelogId != null,AttrGroupEntity::getCatelogId,catelogId);
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(lqw);
        List<Long> attrGroupIds = attrGroupEntities.stream().map((attrGroup) -> {
            return attrGroup.getAttrGroupId();
        }).collect(Collectors.toList());

        //2.2、找到这些分组关联的规格参数
        LambdaQueryWrapper<AttrAttrgroupRelationEntity> lqw2 = new LambdaQueryWrapper<>();
        lqw2.in(attrGroupIds != null && attrGroupIds.size()>0,AttrAttrgroupRelationEntity::getAttrGroupId,attrGroupIds);
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(lqw2);
        List<Long> attrIds = relationEntities.stream().map((relationEntity) -> {
            return relationEntity.getAttrId();
        }).collect(Collectors.toList());

        //2.3、从当前分类的所有属性中移除这些规格参数
        LambdaQueryWrapper<AttrEntity> lqw3 = new LambdaQueryWrapper<>();
        lqw3.eq(AttrEntity::getCatelogId,catelogId).eq(AttrEntity::getAttrType,ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        lqw3.notIn(attrIds != null && attrIds.size()>0,AttrEntity::getAttrId,attrIds);
        //获取查询时的key
        String key = (String) params.get("key");
        if(StringUtils.isNotEmpty(key)){
            lqw3.and((wrapper) -> {
                wrapper.eq(AttrEntity::getAttrId,key).or().like(AttrEntity::getAttrName,key);
            });
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params),lqw3);

        return new PageUtils(page);
    }

}