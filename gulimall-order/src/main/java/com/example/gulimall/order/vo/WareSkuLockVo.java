package com.example.gulimall.order.vo;

import lombok.Data;

import java.util.List;

/**
 * @Date: 2022/10/27 20:55
 *
 * 保存订单后，进行锁库存
 * 锁库存所需要的数据vo
 */
@Data
public class WareSkuLockVo {

    //订单号
    private String orderSn;

    //需要锁住的所有库存信息
    private List<OrderItemVo> locks;
}
