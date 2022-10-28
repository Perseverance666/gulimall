package com.example.gulimall.order.vo;

import com.example.gulimall.order.entity.OrderEntity;
import lombok.Data;

/**
 * @Date: 2022/10/26 20:41
 *
 * 提交订单后，返回的数据vo
 */
@Data
public class SubmitOrderRespVo {

    /**
     * 订单信息
     */
    private OrderEntity order;

    /**
     * 状态码：
     * 0：成功
     * 1：令牌验证失败
     * 2：验价失败
     */
    private Integer code;
}
