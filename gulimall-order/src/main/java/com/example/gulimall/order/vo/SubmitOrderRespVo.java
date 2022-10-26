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

    private OrderEntity order;

    //成功：0   错误：状态码
    private Integer code;
}
