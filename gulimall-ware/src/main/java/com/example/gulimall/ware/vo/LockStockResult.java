package com.example.gulimall.ware.vo;

import lombok.Data;

/**
 * @Date: 2022/10/27 21:13
 *
 * 锁库存后，返回的信息
 */
@Data
public class LockStockResult {

    //锁住的商品
    private Long skuId;

    //锁住商品的数量
    private Integer num;

    //是否锁住
    private Boolean locked;
}
