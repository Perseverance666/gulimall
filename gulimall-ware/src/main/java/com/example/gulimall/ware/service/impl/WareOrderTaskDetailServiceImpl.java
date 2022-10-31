package com.example.gulimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.constant.OrderConstant;
import com.example.common.constant.WareConstant;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.ware.dao.WareOrderTaskDetailDao;
import com.example.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.example.gulimall.ware.service.WareOrderTaskDetailService;


@Service("wareOrderTaskDetailService")
public class WareOrderTaskDetailServiceImpl extends ServiceImpl<WareOrderTaskDetailDao, WareOrderTaskDetailEntity> implements WareOrderTaskDetailService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareOrderTaskDetailEntity> page = this.page(
                new Query<WareOrderTaskDetailEntity>().getPage(params),
                new QueryWrapper<WareOrderTaskDetailEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 根据库存工作单id，查询已锁定(lock_status = 1)的库存工作单详情信息
     * @param taskId
     * @return
     */
    @Override
    public List<WareOrderTaskDetailEntity> listByTaskIdWithLocked(Long taskId) {
        LambdaQueryWrapper<WareOrderTaskDetailEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(WareOrderTaskDetailEntity::getTaskId,taskId)
                .eq(WareOrderTaskDetailEntity::getLockStatus, WareConstant.LockStatusEnum.LOCKED.getCode());
        List<WareOrderTaskDetailEntity> list = this.list(lqw);
        return list;
    }

}