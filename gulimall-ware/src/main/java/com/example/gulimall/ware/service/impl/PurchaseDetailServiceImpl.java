package com.example.gulimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.ware.dao.PurchaseDetailDao;
import com.example.gulimall.ware.entity.PurchaseDetailEntity;
import com.example.gulimall.ware.service.PurchaseDetailService;


@Service("purchaseDetailService")
public class PurchaseDetailServiceImpl extends ServiceImpl<PurchaseDetailDao, PurchaseDetailEntity> implements PurchaseDetailService {

    /**
     * 查询采购需求
     * 库存系统，采购单维护，采购需求，列表展示
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<PurchaseDetailEntity> lqw = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");
        if(StringUtils.isNotEmpty(key)){
            lqw.and((wrapper) -> {
                wrapper.eq(PurchaseDetailEntity::getPurchaseId,key).or().eq(PurchaseDetailEntity::getSkuId,key);
            });
        }

        String status = (String) params.get("status");
        if(StringUtils.isNotEmpty(status)){
            lqw.eq(PurchaseDetailEntity::getStatus,status);
        }

        String wareId = (String) params.get("wareId");
        if(StringUtils.isNotEmpty(wareId)){
            lqw.eq(PurchaseDetailEntity::getWareId,wareId);
        }

        IPage<PurchaseDetailEntity> page = this.page(new Query<PurchaseDetailEntity>().getPage(params),lqw);
        return new PageUtils(page);
    }

}