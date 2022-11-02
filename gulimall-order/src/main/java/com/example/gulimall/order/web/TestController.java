package com.example.gulimall.order.web;

import com.example.gulimall.order.entity.OrderEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

/**
 * @Date: 2022/10/25 13:48
 */

@Controller
public class TestController {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/{page}.html")
    public String listPage(@PathVariable String page){
        return page;
    }

    @ResponseBody
    @GetMapping("/test/createOrder")
    public String testSendMessage(){
        OrderEntity order = new OrderEntity();
        order.setOrderSn(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend("order-event-exchange",
                "order.create.order",order);
        return "ok";
    }
}
