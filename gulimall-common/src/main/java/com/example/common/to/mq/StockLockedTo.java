package com.example.common.to.mq;

import lombok.Data;

import java.util.List;

/**
 * @Date: 2022/10/31 15:43
 *
 * 锁定库存成功时，发送的mq消息
 */

@Data
public class StockLockedTo {

    //库存工作单的id
    private Long wareOrderTaskId;

    //库存工作单的详情信息
    private StockDetailTo detail;
}
