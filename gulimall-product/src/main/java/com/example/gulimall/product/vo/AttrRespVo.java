package com.example.gulimall.product.vo;

import lombok.Data;

/**
 * @Date: 2022/9/22 16:56
 */

@Data
public class AttrRespVo extends AttrVo{
    private String catelogName;

    private String groupName;

    private Long attrGroupId;

    private Long[] catelogPath;
}
