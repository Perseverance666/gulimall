package com.example.gulimall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Date: 2022/9/27 17:05
 */

@Data
public class PurchaseDoneVo {
    @NotNull
    private Long id;         //采购单id
    private List<PurchaseItemDoneVo> items;     //采购项 完成/失败的需求详情
}
