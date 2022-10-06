package com.example.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gulimall.product.service.CategoryBrandRelationService;
import com.example.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.CategoryDao;
import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取所有分类及子分类
     * 并返回json树形结构
     * 商品系统，分类维护 以及 平台属性左侧的列表展示
     *
     * @return
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        //查询所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //查询一级分类，并设置其子分类
        List<CategoryEntity> level1Menus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren(getChildren(menu,entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    /**
     * 商品系统，分类维护，批量删除
     * @param asList
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 检查当前删除的菜单，是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }

    /**
     * 找出指定catelogId的全路径
     * @param catelogId
     * @return
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        //结果是[225,25,2]，注意翻转
        Collections.reverse(parentPath);

        return parentPath.toArray(new Long[parentPath.size()]);
    }
    /**
     * 递归查询 返回指定catelogId的所有父节点catelogId
     * 返回结果例：[225,25,2]
     * @param catelogId
     * @param paths
     * @return
     */
    public List<Long> findParentPath(Long catelogId,List<Long> paths){
        //先将该catelogId添加到paths中
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid() != 0){
            //有父节点
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());

        //TODO 更新其他关联
    }

    /**
     * 查询所有一级分类
     * @return
     */
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        LambdaQueryWrapper<CategoryEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(CategoryEntity::getCatLevel,1);
        List<CategoryEntity> entities = this.baseMapper.selectList(lqw);
        return entities;
    }

    /**
     * 返回map类型数据，key为一级分类id，value为Catelog2Vo类型数据
     * @return
     */
    @Override
    public Map<String,List<Catelog2Vo>>  getCatalogJson() {
        //TODO 优化：将数据库由原来的多次查询变为一次查询
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1、查询所有一级分类
        List<CategoryEntity> level1Categories = getCategoryEntitiesByParentCid(selectList,0L);
        Map<String, List<Catelog2Vo>> map = null;
        if(level1Categories != null){

            //2、封装成map，key为一级分类id，value为Catelog2Vo类型数据
            map = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //查询该一级分类下的二级分类
                List<CategoryEntity> level2Categories = getCategoryEntitiesByParentCid(selectList,v.getCatId());

                //将所有2级分类封装成Catelog2Vo
                List<Catelog2Vo> catelog2Vos = null;
                if (level2Categories != null) {
                    catelog2Vos = level2Categories.stream().map(l2 -> {
                        Catelog2Vo catelog2Vo = new Catelog2Vo();
                        catelog2Vo.setCatalog1Id(v.getCatId().toString());
                        catelog2Vo.setId(l2.getCatId().toString());
                        catelog2Vo.setName(l2.getName());

                        //查询该2级分类下的所有3级分类
                        List<CategoryEntity> level3Categories= getCategoryEntitiesByParentCid(selectList,l2.getCatId());
                        List<Catelog2Vo.Catelog3Vo> catelog3Vos = null;
                        if(level3Categories != null){
                            //封装Catelog3Vo类型数据
                            catelog3Vos = level3Categories.stream().map(l3 -> {
                                Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo();
                                catelog3Vo.setCatalog2Id(l2.getCatId().toString());
                                catelog3Vo.setId(l3.getCatId().toString());
                                catelog3Vo.setName(l3.getName());
                                return catelog3Vo;
                            }).collect(Collectors.toList());
                        }

                        catelog2Vo.setCatalog3List(catelog3Vos);
                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }

                return catelog2Vos;

            }));
        }

        return map;
    }


    private List<CategoryEntity> getCategoryEntitiesByParentCid(List<CategoryEntity> selectList,Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid)
                .collect(Collectors.toList());
        return collect;
    }


    /**
     * 递归找出该分类下的所有子类
     *
     * @param root
     * @param all
     * @return
     */
    public List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter((category) -> {
            return category.getParentCid() == root.getCatId();
        }).map((menu) -> {
            menu.setChildren(getChildren(menu,all));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }
}