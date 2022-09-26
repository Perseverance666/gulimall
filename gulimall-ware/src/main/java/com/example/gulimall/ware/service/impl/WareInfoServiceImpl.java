package com.example.gulimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.ware.dao.WareInfoDao;
import com.example.gulimall.ware.entity.WareInfoEntity;
import com.example.gulimall.ware.service.WareInfoService;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    /**
     * 仓库列表
     * 库存系统，仓库维护，列表展示
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        LambdaQueryWrapper<WareInfoEntity> lqw = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");
        if(StringUtils.isNotEmpty(key)){
            lqw.eq(WareInfoEntity::getId,key)
                    .or().like(WareInfoEntity::getName,key)
                    .or().like(WareInfoEntity::getAddress,key)
                    .or().like(WareInfoEntity::getAreacode,key);
        }

        IPage<WareInfoEntity> page = this.page(new Query<WareInfoEntity>().getPage(params),lqw);
        return new PageUtils(page);
    }

}