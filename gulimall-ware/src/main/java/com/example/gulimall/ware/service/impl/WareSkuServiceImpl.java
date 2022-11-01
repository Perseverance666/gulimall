package com.example.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.constant.OrderConstant;
import com.example.common.constant.WareConstant;
import com.example.common.exception.RRException;
import com.example.common.to.SkuHasStockTo;
import com.example.common.to.mq.OrderTo;
import com.example.common.to.mq.StockDetailTo;
import com.example.common.to.mq.StockLockedTo;
import com.example.common.utils.R;
import com.example.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.example.gulimall.ware.entity.WareOrderTaskEntity;
import com.example.gulimall.ware.feign.OrderFeignService;
import com.example.gulimall.ware.feign.ProductFeignService;
import com.example.gulimall.ware.service.WareOrderTaskDetailService;
import com.example.gulimall.ware.service.WareOrderTaskService;
import com.example.gulimall.ware.vo.OrderItemVo;
import com.example.gulimall.ware.vo.WareSkuLockVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
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
    @Autowired
    private WareOrderTaskService wareOrderTaskService;
    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderFeignService orderFeignService;

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
     *
     * 库存解锁的场景
     * 1、下订单成功，订单过期没有支付被系统自动取消、被用户手动取消。都要解锁库存
     *   1)、锁库存成功后，下单成功，发消息给mq告诉订单创建成功(消息进入order.delay.queue)
     *   2)、经过1分钟订单过期，或者被用户手动取消(消息进入order.release.order.queue)
     *   3)、orderCloseListener监听到消息，开始关闭订单
     *   4)、关闭订单后，发消息给mq告诉解锁库存(消息进入stock.release.stock.queue)
     *   5)、handleOrderCloseRelease监听到消息，由于订单关闭，开始解锁库存
     *
     * 2、下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁。
     *   1)、锁库存成功后，发消息给mq告诉锁定库存成功(消息进入stock.delay.queue)
     *   2)、经过2分钟后(消息进入stock.release.stock.queue)，开始检测库存工作单和订单信息
     *   3)、若库存工作详情单状态为已锁定，没有订单或者订单为已取消状态，开始解锁库存
     *
     * @param vo
     * @return
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        //1、先保存库存工作单ware_order_task，用于追溯
        WareOrderTaskEntity task = new WareOrderTaskEntity();
        task.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(task);

        List<OrderItemVo> locks = vo.getLocks();
        locks.forEach(sku -> {
            Boolean skuStock = false; //默认当前sku没有锁住
            //2、先去查看是否有库存
            Long skuId = sku.getSkuId();
            List<Long> wareIds = this.baseMapper.listWareIdHasSkuStock(skuId);
            if (wareIds == null || wareIds.size() == 0) {
                throw new RRException(skuId + "商品没有库存了");
            }
            for (Long wareId : wareIds) {
                //3、尝试锁库存
                Long count = this.baseMapper.lockSkuStock(skuId, wareId, sku.getCount());
                if (count != 0) {
                    //3.1、锁库存成功
                    skuStock = true;
                    //3.1.1、保存库存工作单详细内容，用于追溯。
                    WareOrderTaskDetailEntity taskDetail = new WareOrderTaskDetailEntity();
                    taskDetail.setSkuId(skuId);
                    taskDetail.setWareId(wareId);
                    taskDetail.setTaskId(task.getId());
                    taskDetail.setSkuNum(sku.getCount());
                    taskDetail.setLockStatus(1);
                    wareOrderTaskDetailService.save(taskDetail);

                    //3.1.2、给mq发消息，告诉锁定库存成功
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setWareOrderTaskId(task.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetail,stockDetailTo);
                    stockLockedTo.setDetail(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                    break;
                } else {
                    //3.2、当前仓库锁失败，尝试下一个仓库
                }
            }
            //3.3、锁定该sku失败
            if (skuStock == false) {
                throw new RRException(skuId + "商品没有库存了");
            }
        });

        //4、走到这里就是成功锁库存了。若失败，抛异常，回滚
        return true;
    }

    /**
     * 监听解锁库存消息 抽取的解锁库存方法
     *
     * 库存工作详情单有数据(库存锁住了) -> 库存工作详情单状态为已锁定 -> 没有订单或者订单为已取消状态 才能解锁
     * @param stockLockedTo
     */
    public void tryUnLockStock(StockLockedTo stockLockedTo){
        StockDetailTo detail = stockLockedTo.getDetail();
        WareOrderTaskDetailEntity taskDetail = wareOrderTaskDetailService.getById(detail.getId());
        if(taskDetail == null || taskDetail.getLockStatus() != WareConstant.LockStatusEnum.LOCKED.getCode()){
            //1.1、库存工作详情单中没有数据，即没有锁住库存，无需解锁
            //1.2、库存工作详情单状态不是已锁定，是已解锁或者已扣除，不能解锁
        }else{
            //1.3、库存工作详情单中有数据，证明库存锁定成功
            //2、查询是否有订单
            WareOrderTaskEntity task = wareOrderTaskService.getById(stockLockedTo.getWareOrderTaskId());
            R r = orderFeignService.getOrderByOrderSn(task.getOrderSn());
            if(r.getCode() == 0){
                OrderTo order = r.getData("order", new TypeReference<OrderTo>() {});
                if(order == null || order.getStatus() == OrderConstant.OrderStatusEnum.CANCLED.getCode()){
                    //3、没有订单信息，或者订单为已取消状态，需要解锁库存
                    unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(),detail.getId());
                }
            }else{
                throw new RRException("远程调用order的getOrderByOrderSn失败");
            }

        }
    }

    /**
     * 监听解锁库存消息 抽取的由于关闭订单而解锁库存方法
     *
     * @param orderTo
     */
    @Override
    public void tryUnLockStockAfterCloseOrder(OrderTo orderTo) {
        //查询库存工作单id
        WareOrderTaskEntity task = wareOrderTaskService.getOrderTaskByOrderSn(orderTo.getOrderSn());
        Long taskId = task.getId();
        //根据库存工作单id，查询已锁定的库存工作单详情信息
        List<WareOrderTaskDetailEntity> taskDetails = wareOrderTaskDetailService.listByTaskIdWithLocked(taskId);
        for (WareOrderTaskDetailEntity taskDetail : taskDetails) {
            //解锁库存
            unLockStock(taskDetail.getSkuId(),taskDetail.getWareId(),taskDetail.getSkuNum(),taskDetail.getId());
        }
    }

    /**
     * 解锁库存
     * @param skuId
     * @param wareId
     * @param skuNum
     * @param detailId
     */
    @Transactional
    public void unLockStock(Long skuId, Long wareId, Integer skuNum, Long detailId) {
        //1、库存解锁
        this.baseMapper.unLockStock(skuId,wareId,skuNum);

        //2、更新库存工作详情单信息
        WareOrderTaskDetailEntity taskDetail = new WareOrderTaskDetailEntity();
        taskDetail.setId(detailId);
        //变为已解锁
        taskDetail.setLockStatus(2);
        wareOrderTaskDetailService.updateById(taskDetail);
    }


}