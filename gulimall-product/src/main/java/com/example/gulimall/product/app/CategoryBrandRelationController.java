package com.example.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gulimall.product.entity.BrandEntity;
import com.example.gulimall.product.vo.BrandVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.gulimall.product.entity.CategoryBrandRelationEntity;
import com.example.gulimall.product.service.CategoryBrandRelationService;
import com.example.common.utils.PageUtils;
import com.example.common.utils.R;



/**
 * 品牌分类关联
 *
 * @author liu
 * @email liu@gmail.com
 * @date 2022-09-16 13:25:00
 */
@RestController
@RequestMapping("product/categorybrandrelation")
public class CategoryBrandRelationController {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;


    /**
     * 获取分类关联的品牌
     * 商品系统，商品维护，发布商品，点击选择分类后的选择品牌展示
     * 商品系统，商品维护，spu管理，点击分类后的品牌展示
     * 商品系统，商品维护，商品管理，点击分类后的品牌展示
     * @param catId
     * @return
     */
    @GetMapping("/brands/list")
    public R relationBrandList(@RequestParam("catId") Long catId){
        List<BrandEntity> brandEntities = categoryBrandRelationService.getBrandsByCatId(catId);

        List<BrandVo> brandVos = brandEntities.stream().map((brandEntity) -> {
            BrandVo brandVo = new BrandVo();
            brandVo.setBrandId(brandEntity.getBrandId());
            brandVo.setBrandName(brandEntity.getName());
            return brandVo;
        }).collect(Collectors.toList());

        return R.ok().put("data",brandVos);
    }

    /**
     * 获取品牌关联的分类
     * 商品系统，品牌管理，关联分类，展示列表
     */
    @GetMapping("/catelog/list")
    //@RequiresPermissions("product:categorybrandrelation:list")
    public R catelogList(@RequestParam("brandId") Long brandId){
        LambdaQueryWrapper<CategoryBrandRelationEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(brandId != null,CategoryBrandRelationEntity::getBrandId,brandId);
        List<CategoryBrandRelationEntity> data = categoryBrandRelationService.list(lqw);

        return R.ok().put("data", data);
    }

    /**
     * 新增品牌与分类关联关系
     * 商品系统，品牌管理，关联分类，新增分类功能
     */
    @PostMapping("/save")
    //@RequiresPermissions("product:categorybrandrelation:save")
    public R save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
        categoryBrandRelationService.saveDetail(categoryBrandRelation);

        return R.ok();
    }
    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:categorybrandrelation:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = categoryBrandRelationService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("product:categorybrandrelation:info")
    public R info(@PathVariable("id") Long id){
		CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok().put("categoryBrandRelation", categoryBrandRelation);
    }



    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:categorybrandrelation:update")
    public R update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.updateById(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:categorybrandrelation:delete")
    public R delete(@RequestBody Long[] ids){
		categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
