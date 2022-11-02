package com.example.gulimall.order.listener;

import com.example.gulimall.order.entity.OrderEntity;
import com.example.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @Date: 2022/10/31 19:55
 */


@RabbitListener(queues = "order.release.order.queue")
@Service
public class OrderCloseListener {
    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void orderCloseListener(OrderEntity order, Message message, Channel channel) throws IOException {
        System.out.println("收到过期的订单信息，准备关闭订单："+order.getOrderSn()+"==>"+order.getId());
        try {
            orderService.closeOrder(order);
            //TODO 手动调用支付宝收单；(查看支付宝官方文档)
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }
}
