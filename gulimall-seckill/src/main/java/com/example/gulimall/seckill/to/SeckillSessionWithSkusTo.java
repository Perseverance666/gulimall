package com.example.gulimall.seckill.to;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @Date: 2022/11/4 19:55
 * 关联秒杀商品的秒杀场次vo
 */

@Data
public class SeckillSessionWithSkusTo {
    private Long id;
    /**
     * 场次名称
     */
    private String name;
    /**
     * 每日开始时间
     */
    private Date startTime;
    /**
     * 每日结束时间
     */
    private Date endTime;
    /**
     * 启用状态
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;


    private List<SeckillSkuTo> relationSkus;
}
