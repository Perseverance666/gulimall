package com.example.gulimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.exception.RRException;
import com.example.common.to.SkuHasStockTo;
import com.example.common.utils.R;
import com.example.gulimall.ware.feign.ProductFeignService;
import com.example.gulimall.ware.vo.OrderItemVo;
import com.example.gulimall.ware.vo.SkuWareHasStockVo;
import com.example.gulimall.ware.vo.WareSkuLockVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.ware.dao.WareSkuDao;
import com.example.gulimall.ware.entity.WareSkuEntity;
import com.example.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private ProductFeignService productFeignService;
    /**
     * 查询商品库存
     * 库存系统，商品库存，列表展示
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<WareSkuEntity> lqw = new LambdaQueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(StringUtils.isNotEmpty(skuId)){
            lqw.eq(WareSkuEntity::getSkuId,skuId);
        }
        String wareId = (String) params.get("wareId");
        if(StringUtils.isNotEmpty(wareId)){
            lqw.eq(WareSkuEntity::getWareId,wareId);
        }

        IPage<WareSkuEntity> page = this.page(new Query<WareSkuEntity>().getPage(params),lqw);
        return new PageUtils(page);
    }

    /**
     * 完成采购
     * 模拟采购人员app实现完成采购时的加入库存功能
     * @param skuId
     * @param wareId
     * @param skuNum
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //先判断库存中是否有货
        LambdaQueryWrapper<WareSkuEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(skuId != null,WareSkuEntity::getSkuId,skuId)
                .eq(wareId != null,WareSkuEntity::getWareId,wareId);
        List<WareSkuEntity> wareSkuEntities = this.list(lqw);
        if(wareSkuEntities == null ||wareSkuEntities.size() == 0){
            //库存中没有货，新增操作
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //TODO 远程查询sku的名字，如果失败，整个事务无需回滚
            //1、自己catch异常
            //2、TODO 高级篇讲其他方法
            try{
                R info = productFeignService.info(skuId);
                Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");
                if(info.getCode() == 0){
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }

            }catch (Exception e){

            }

            this.baseMapper.insert(wareSkuEntity);
        }else {
            //库存中有货，修改操作
            this.baseMapper.addStock(skuId,wareId,skuNum);
        }

    }

    /**
     * 根据skuIds来查询所有的sku是否有库存
     * 商品上架时调用此方法
     * @param skuIds
     * @return
     */
    @Override
    public List<SkuHasStockTo> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockTo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockTo to = new SkuHasStockTo();
            Long count = this.baseMapper.getSkuStock(skuId);
            to.setSkuId(skuId);
            to.setHasStock(count==null ? false : count>0);
            return to;
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 保存订单之后的锁库存操作
     * 原本应该按照下单的收货地址，找到一个就近仓库，锁定库存。(此处就省略了)
     * @param vo
     * @return
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {

        List<OrderItemVo> locks = vo.getLocks();
        locks.forEach(sku -> {
            Boolean skuStock = false; //默认当前sku没有锁住
            //1、先去查看是否有库存
            Long skuId = sku.getSkuId();
            List<Long> wareIds = this.baseMapper.listWareIdHasSkuStock(skuId);
            if (wareIds == null || wareIds.size() == 0) {
                throw new RRException(skuId + "商品没有库存了");
            }
            for (Long wareId : wareIds) {
                //2、尝试锁库存
                Long count = this.baseMapper.lockSkuStock(skuId, wareId, sku.getCount());
                if (count != 0) {
                    //2.1、锁库存成功
                    skuStock = true;
                    break;
                } else {
                    //2.2、当前仓库锁失败，尝试下一个仓库
                }
            }
            //2.3、锁定该sku失败
            if (skuStock == false) {
                throw new RRException(skuId + "商品没有库存了");
            }
        });

        //3、走到这里就是成功锁库存了。若失败，抛异常，回滚
        return true;
    }


}