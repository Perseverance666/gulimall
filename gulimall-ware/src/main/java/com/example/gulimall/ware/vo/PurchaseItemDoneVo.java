package com.example.gulimall.ware.vo;

import lombok.Data;

/**
 * @Date: 2022/9/27 17:04
 */

@Data
public class PurchaseItemDoneVo {
    private Long itemId;        //采购项id
    private Integer status;     //采购项状态
    private String reason;      //失败原因
}
