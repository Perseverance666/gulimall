package com.example.gulimall.order.web;

import com.example.common.exception.RRException;
import com.example.gulimall.order.service.OrderService;
import com.example.gulimall.order.vo.OrderConfirmVo;
import com.example.gulimall.order.vo.OrderSubmitVo;
import com.example.gulimall.order.vo.SubmitOrderRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){
        try{
            //1、下单
            SubmitOrderRespVo respVo = orderService.submitOrder(vo);
            System.out.println("订单提交的数据..."+vo);
            if(respVo.getCode() == 0){
                //2.1、下单成功，跳转到支付页面
                model.addAttribute("submitOrderResp",respVo);
                return "pay";
            }else {
                //2.2、由于令牌验证失败或验价失败，导致下单失败，退回订单确认页重新确认订单信息
                String msg = "下单失败；";
                switch (respVo.getCode()){
                    case 1: msg += "订单信息过期，请刷新再次提交"; break;
                    case 2: msg += "订单商品价格发生变化，请确认后再次提交";
                }
                redirectAttributes.addFlashAttribute("msg",msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        }catch (Exception e){
            //2.3、由于某个商品没有库存，导致下单失败，退回订单确认页重新确认订单信息
            if(e instanceof RRException){
                String message = e.getMessage();
                redirectAttributes.addFlashAttribute("msg",message);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }

    }
}
