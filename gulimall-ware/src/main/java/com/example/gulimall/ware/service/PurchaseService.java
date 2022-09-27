package com.example.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.utils.PageUtils;
import com.example.common.utils.R;
import com.example.gulimall.ware.entity.PurchaseEntity;
import com.example.gulimall.ware.vo.MergeVo;
import com.example.gulimall.ware.vo.PurchaseDoneVo;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author ll
 * @email ll@gmail.com
 * @date 2022-08-13 14:22:38
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceivePurchase(Map<String, Object> params);

    R mergePurchase(MergeVo vo);

    R received(List<Long> ids);

    void done(PurchaseDoneVo vo);
}

