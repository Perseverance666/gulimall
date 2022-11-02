package com.example.gulimall.order.web;

import com.alipay.api.AlipayApiException;
import com.example.gulimall.order.service.OrderService;
import com.example.gulimall.order.utils.AlipayTemplate;
import com.example.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Date: 2022/11/1 17:04
 */

@Controller
public class PayWebController {
    @Autowired
    private AlipayTemplate alipayTemplate;
    @Autowired
    private OrderService orderService;

    /**
     * 1、将支付页让浏览器展示。
     * 2、支付成功以后，我们要跳到用户的订单列表页
     *
     * 支付流程：点击支付处理parOrder请求 -> 将支付需要的相关信息payVo传给alipayTemplate -> 跳转到返回的页面开始支付
     * -> 支付成功后，异步通知处理notify-url请求 -> 验签 -> 保存流水，修改订单信息 -> 最后跳转到订单列表页
     * @param orderSn
     * @return
     * @throws AlipayApiException
     */
    @ResponseBody
    @GetMapping(value = "/payOrder",produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        PayVo payVo = orderService.getOrderPayByOrderSn(orderSn);
        String pay = alipayTemplate.pay(payVo);
        return pay;
    }
}
