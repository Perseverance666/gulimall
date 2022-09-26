package com.example.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @Date: 2022/9/26 19:54
 */

@Data
public class MergeVo {
    private Long purchaseId;

    private List<Long> items;
}
