package com.example.gulimall.order.web;

import com.example.gulimall.order.service.OrderService;
import com.example.gulimall.order.vo.OrderConfirmVo;
import com.example.gulimall.order.vo.OrderSubmitVo;
import com.example.gulimall.order.vo.SubmitOrderRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.concurrent.ExecutionException;

/**
 * @Date: 2022/10/25 15:36
 */

@Controller
public class OrderWebController {
    @Autowired
    private OrderService orderService;

    /**
     * 返回订单确认页需要的数据
     * @param model
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData",orderConfirmVo);
        return "confirm";
    }

    /**
     * 下单功能
     * @param vo
     * @param model
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo,Model model){
        SubmitOrderRespVo respVo = orderService.submitOrder(vo);
        if(respVo.getCode() == 0){
            //下单成功，跳转到支付页面
            return "pay";
        }else {
            //下单失败，退回订单确认页重新确认订单信息
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}
