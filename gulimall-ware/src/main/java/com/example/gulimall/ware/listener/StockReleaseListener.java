package com.example.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.example.common.exception.RRException;
import com.example.common.to.mq.OrderTo;
import com.example.common.to.mq.StockDetailTo;
import com.example.common.to.mq.StockLockedTo;
import com.example.common.utils.R;
import com.example.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.example.gulimall.ware.entity.WareOrderTaskEntity;
import com.example.gulimall.ware.feign.OrderFeignService;
import com.example.gulimall.ware.feign.ProductFeignService;
import com.example.gulimall.ware.service.WareOrderTaskDetailService;
import com.example.gulimall.ware.service.WareOrderTaskService;
import com.example.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @Date: 2022/10/31 17:38
 */


@RabbitListener(queues = "stock.release.stock.queue")
@Service
public class StockReleaseListener {
    @Autowired
    private WareSkuService wareSkuService;


    /**
     * 监听解锁库存消息
     *
     * @param stockLockedTo
     * @param message
     * @param channel
     */
    @RabbitHandler
    public void handlerStockLockedRelease(StockLockedTo stockLockedTo,Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存消息，开始解锁库存：");
        try{
            wareSkuService.tryUnLockStock(stockLockedTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            //true为将消息放回队列中
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }

    /**
     * 监听由于订单关闭而解锁库存消息
     * @param orderTo
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("收到订单关闭消息，开始解锁库存：");
        try{
            wareSkuService.tryUnLockStockAfterCloseOrder(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }


}
