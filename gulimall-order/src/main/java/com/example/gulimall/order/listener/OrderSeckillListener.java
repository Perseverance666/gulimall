package com.example.gulimall.order.listener;

import com.example.common.to.mq.SeckillOrderTo;
import com.example.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Date: 2022/11/6 20:16
 * 秒杀成功,快速下单,发mq消息,剩下的下单流程,监听到队列后,慢慢进行
 */

@RabbitListener(queues = "order.seckill.order.queue")
@Component
public class OrderSeckillListener {
    @Autowired
    private OrderService orderService;
    @RabbitHandler
    public void listener(SeckillOrderTo seckillOrder, Channel channel, Message message) throws IOException {

        try{
            System.out.println("准备创建秒杀单的详细信息...");
            orderService.createSeckillOrder(seckillOrder);
            //TODO 手动调用支付宝收单；(查看支付宝官方文档)
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }
}
