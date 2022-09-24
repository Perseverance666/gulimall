package com.example.gulimall.product.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.example.gulimall.product.entity.AttrEntity;
import lombok.Data;

import java.util.List;

/**
 * @Date: 2022/9/24 16:57
 */

@Data
public class AttrGroupWithAttrsVo {
    /**
     * 分组id
     */
    private Long attrGroupId;
    /**
     * 组名
     */
    private String attrGroupName;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 描述
     */
    private String descript;
    /**
     * 组图标
     */
    private String icon;
    /**
     * 所属分类id
     */
    private Long catelogId;

    /**
     * 指定分组下的所有规格参数
     */
    private List<AttrEntity> attrs;

}
