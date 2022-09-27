package com.example.gulimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.WareConstant;
import com.example.common.utils.R;
import com.example.gulimall.ware.entity.PurchaseDetailEntity;
import com.example.gulimall.ware.service.PurchaseDetailService;
import com.example.gulimall.ware.service.WareSkuService;
import com.example.gulimall.ware.vo.MergeVo;
import com.example.gulimall.ware.vo.PurchaseDoneVo;
import com.example.gulimall.ware.vo.PurchaseItemDoneVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.ware.dao.PurchaseDao;
import com.example.gulimall.ware.entity.PurchaseEntity;
import com.example.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService purchaseDetailService;
    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查询未领取的采购单
     * 库存系统，采购单维护，采购需求，进行批量操作的合并整单列表展示
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {
        LambdaQueryWrapper<PurchaseEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(PurchaseEntity::getStatus,WareConstant.PurchaseStatusEnum.CREATED.getCode())
                .or().eq(PurchaseEntity::getStatus,WareConstant.PurchaseStatusEnum.ASSIGNED.getCode());
        IPage<PurchaseEntity> page = this.page(new Query<PurchaseEntity>().getPage(params),lqw);
        return new PageUtils(page);
    }

    /**
     * 合并采购需求
     * 库存系统，采购单维护，采购需求，进行批量操作的合并整单功能
     * @return
     */
    @Transactional
    @Override
    public R mergePurchase(MergeVo vo) {
        Long purchaseId = vo.getPurchaseId();
        //如果没有采购单，就创建一个
        if(purchaseId == null){
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        //TODO 确认采购需求状态是新建，或者已分配才可以合并

        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = vo.getItems().stream().filter((item) -> {
            PurchaseDetailEntity byId = purchaseDetailService.getById(item);
            if(byId.getStatus() == WareConstant.PurchaseDetailStatusEnum.CREATED.getCode() ||
                    byId.getStatus() == WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode()){
                return true;
            }
            return false;
        }).map((item) -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(item);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return purchaseDetailEntity;
        }).collect(Collectors.toList());

        if(collect != null && collect.size() > 0){
            purchaseDetailService.updateBatchById(collect);

            //设置采购单的更新时间
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setId(purchaseId);
            purchaseEntity.setUpdateTime(new Date());
            this.updateById(purchaseEntity);
            return R.ok();

        }else{
            log.error("当前采购需求无法合并");
            return R.error().put("msg","当前采购需求无法合并");
        }
    }

    /**
     * 领取采购单
     * 模拟采购人员app实现领取采购单功能
     * @param ids 采购单id
     * @return
     */
    @Override
    public R received(List<Long> ids) {
        //1、确认当前采购单是新建或者已分配状态
        List<PurchaseEntity> purchaseEntities = this.listByIds(ids);
        List<PurchaseEntity> collect = purchaseEntities.stream().filter((purchaseEntity) -> {
            if (purchaseEntity.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                    purchaseEntity.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map((entity) -> {
            entity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            entity.setUpdateTime(new Date());
            return entity;
        }).collect(Collectors.toList());
        if(collect != null && collect.size() > 0){
            //2、改变采购单的状态
            this.updateBatchById(collect);
            //3、改变采购项的状态
            collect.forEach((item) -> {
                LambdaQueryWrapper<PurchaseDetailEntity> lqw = new LambdaQueryWrapper<>();
                lqw.eq(PurchaseDetailEntity::getPurchaseId,item.getId());
                List<PurchaseDetailEntity> purchaseDetailEntities = purchaseDetailService.list(lqw);
                List<PurchaseDetailEntity> detailEntities = purchaseDetailEntities.stream().map((detailEntity) -> {
                    detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                    return detailEntity;
                }).collect(Collectors.toList());
                purchaseDetailService.updateBatchById(detailEntities);
            });
            return R.ok();
        }else{
            log.error("当前采购单已被领取");
            return R.error().put("msg","当前采购单已被领取");
        }

    }

    /**
     * 完成采购
     * 模拟采购人员app实现完成采购功能
     * @param vo
     * @return
     */
    @Transactional
    @Override
    public void done(PurchaseDoneVo vo) {
        Boolean flag = true;
        //1、改变采购项的状态
        List<PurchaseItemDoneVo> items = vo.getItems();
        List<PurchaseDetailEntity> purchaseDetailEntities = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity purchaseDetailEntity = purchaseDetailService.getById(item.getItemId());
            if(purchaseDetailEntity != null){
                if(item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()){
                    purchaseDetailEntity.setStatus(item.getStatus());
                    flag = false;
                }else{
                    //2、将成功采购的进行入库
                    purchaseDetailEntity.setStatus(item.getStatus());
                    wareSkuService.addStock(purchaseDetailEntity.getSkuId(),purchaseDetailEntity.getWareId(),purchaseDetailEntity.getSkuNum());
                }
                purchaseDetailEntities.add(purchaseDetailEntity);
            }
        }
        purchaseDetailService.updateBatchById(purchaseDetailEntities);

        //3、改变采购单的状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(vo.getId());
        purchaseEntity.setStatus(flag ? WareConstant.PurchaseStatusEnum.FINISH.getCode() : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);

    }


}