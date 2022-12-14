package com.example.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.common.constant.OrderConstant;
import com.example.common.exception.RRException;
import com.example.common.to.mq.OrderTo;
import com.example.common.to.mq.SeckillOrderTo;
import com.example.common.utils.R;
import com.example.common.vo.MemberRespVo;
import com.example.gulimall.order.entity.OrderItemEntity;
import com.example.gulimall.order.entity.PaymentInfoEntity;
import com.example.gulimall.order.feign.CartFeignService;
import com.example.gulimall.order.feign.MemberFeignService;
import com.example.gulimall.order.feign.ProductFeignService;
import com.example.gulimall.order.feign.WareFeignService;
import com.example.gulimall.order.interceptor.LoginInterceptor;
import com.example.gulimall.order.service.OrderItemService;
import com.example.gulimall.order.service.PaymentInfoService;
import com.example.gulimall.order.to.OrderCreateTo;
import com.example.gulimall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.order.dao.OrderDao;
import com.example.gulimall.order.entity.OrderEntity;
import com.example.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private MemberFeignService memberFeignService;
    @Autowired
    private CartFeignService cartFeignService;
    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    private WareFeignService wareFeignService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * ??????????????????????????????
     * @param vo
     * @return
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //1?????????????????????
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setAlipayTradeNo(vo.getTrade_no());
        paymentInfo.setOrderSn(vo.getOut_trade_no());
        paymentInfo.setCallbackTime(vo.getNotify_time());
        paymentInfo.setPaymentStatus(vo.getTrade_status());
        paymentInfoService.save(paymentInfo);

        //2?????????????????????
        //2.1???????????????????????????????????????????????????TRADE_SUCCESS???????????????????????????
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            String orderSn = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatusByOrderSn(orderSn,OrderConstant.OrderStatusEnum.PAYED.getCode());
            return "success";
        }else{
            System.out.println("??????????????????...");
            return "error";
        }

    }

    /**
     * ???????????????????????????????????????????????????????????????
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginInterceptor.loginUser.get();
        LambdaQueryWrapper<OrderEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(OrderEntity::getMemberId,memberRespVo.getId());
        lqw.orderByDesc(OrderEntity::getId);
        IPage<OrderEntity> page = this.page(new Query<OrderEntity>().getPage(params),lqw);

        List<OrderEntity> collect = page.getRecords().stream().map(order -> {
            LambdaQueryWrapper<OrderItemEntity> lqw2 = new LambdaQueryWrapper<>();
            lqw2.eq(OrderItemEntity::getOrderSn, order.getOrderSn());
            List<OrderItemEntity> orderItems = orderItemService.list(lqw2);
            order.setItemEntities(orderItems);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(collect);
        return new PageUtils(page);
    }

    /**
     * ??????????????????????????????????????????
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPayByOrderSn(String orderSn) {
        PayVo payVo = new PayVo();
        //?????????????????????
        payVo.setOut_trade_no(orderSn);

        OrderEntity order = this.getOrderByOrderSn(orderSn);
        BigDecimal payAmount = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        //??????????????????
        payVo.setTotal_amount(payAmount.toString());

        List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = order_sn.get(0);
        //???????????????????????????????????????
        payVo.setSubject(entity.getSkuName());

        //??????????????????????????????????????????????????????
        payVo.setBody(entity.getSkuAttrsVals());
        return payVo;
    }


    /**
     * ????????????
     *
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ???????????????????????????????????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????? ??????????????????????????????
     * @param oldOrder
     */
    @Override
    public void closeOrder(OrderEntity oldOrder) {
        //???????????????????????????????????????????????????????????????order????????????????????????????????????????????????status??????
        OrderEntity order = this.getById(oldOrder.getId());
        //??????????????????????????????????????????
        if(order.getStatus() == OrderConstant.OrderStatusEnum.CREATE_NEW.getCode()){
            System.out.println("???????????????????????????????????????????????????"+order.getOrderSn()+"==>"+order.getId());
            //1???????????????
            order.setStatus(OrderConstant.OrderStatusEnum.CANCLED.getCode());
            this.updateById(order);
            //2????????????????????????????????????????????????????????????
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(order,orderTo);
            try {
                //TODO ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                //TODO ?????????????????????????????????????????????????????????
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            }catch (Exception e){
                //TODO ????????????????????????????????????????????????
            }
        }
    }

    /**
     * ????????????????????????????????????
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberRespVo loginUser = LoginInterceptor.loginUser.get();
        System.out.println("?????????...." + Thread.currentThread().getId());
        //?????????????????????
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        //1????????????????????????????????????????????????
        CompletableFuture<Void> memberFuture = CompletableFuture.runAsync(() -> {
            System.out.println("member??????...." + Thread.currentThread().getId());
            //?????????????????????????????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(requestAttributes);

            List<MemberAddressVo> addresses = memberFeignService.getAddresses(loginUser.getId());
            orderConfirmVo.setAddress(addresses);
        }, executor);

        //2???????????????????????????????????????????????????????????????Feign??????????????????????????????
        CompletableFuture<Void> cartFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("cart??????...." + Thread.currentThread().getId());
            //?????????????????????????????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(requestAttributes);

            List<OrderItemVo> items = cartFeignService.getCurrentUserCheckedCartItems();
            orderConfirmVo.setItems(items);
            return items;
        }, executor).thenAcceptAsync((items) -> {
            //2.1?????????????????????????????????????????????????????????????????????????????????
            List<Long> skuIds = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R hasStock = wareFeignService.getSkusHasStock(skuIds);
            List<SkuStockVo> data = hasStock.getData("data", new TypeReference<List<SkuStockVo>>() {});
            if(data != null){
                Map<Long, Boolean> stocks = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                //2.2?????????orderConfirmVo???????????????
                orderConfirmVo.setStocks(stocks);
            }
        },executor);

        //3???????????????????????????
        orderConfirmVo.setIntegration(loginUser.getIntegration());

        //4?????????????????????????????????

        //5???????????????????????????redis???30min
        String orderToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId(),orderToken,30, TimeUnit.MINUTES);
        orderConfirmVo.setOrderToken(orderToken);

        CompletableFuture.allOf(memberFuture,cartFuture).get();
        return orderConfirmVo;
    }

    /**
     * ????????????
     *
     * 1??????????????? -> ???????????? -> ??????(??????) -> ?????????????????? -> ?????????
     * 2???????????????????????????????????????mq????????????????????????(????????????order.delay.queue)
     * -> ??????1??????????????????(????????????order.release.order.queue)
     * -> orderCloseListener????????????????????????????????????
     * -> ??????????????????????????????mq??????????????????(????????????stock.release.stock.queue)
     * -> handleOrderCloseRelease?????????????????????????????????????????????????????????
     *
     * ???????????????
     * 1?????????????????????????????????????????????????????????????????????????????????????????????????????????
     * 2????????????????????? ???????????????????????????+??????????????????
     * @param vo
     * @return
     */
//    @GlobalTransactional    //??????????????????
    @Transactional
    @Override
    public SubmitOrderRespVo submitOrder(OrderSubmitVo vo) {
        SubmitOrderRespVo response = new SubmitOrderRespVo();
        MemberRespVo loginUser = LoginInterceptor.loginUser.get();
        //1?????????????????????token
        //1.1?????????lua?????????????????????????????????????????????????????????
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        Long result = redisTemplate.execute(new DefaultRedisScript<>(luaScript, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId()), orderToken);
        if (result == 0L){
            //1.2?????????????????????
            response.setCode(1);
            return response;
        }else{
            //1.3?????????????????????

            //2???????????????
            OrderCreateTo order = createOrder(vo);

            //3?????????
            BigDecimal payAmount = order.getPayPrice();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //3.1???????????????

                //4?????????????????????
                saveOrder(order);

                //5???????????????????????????????????????????????????????????????
                //5.1????????????????????????????????????
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getOrderItems().stream().map(orderItem -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(orderItem.getSkuId());
                    orderItemVo.setCount(orderItem.getSkuQuantity());
                    orderItemVo.setTitle(orderItem.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(locks);

                //5.2???????????????ware???orderLockStock??????????????????
                //??????????????????????????????????????????????????????????????????????????????
                //????????????????????????????????????????????????????????????????????????????????????
                //???????????????????????????????????????????????????  ??????
                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if(r.getCode() == 0){
                    //???????????????
                    response.setOrder(order.getOrder());
                    response.setCode(0);
//                    int i = 10/0; //??????????????????????????????????????????
                    //6??????mq???????????????????????????????????????
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    return response;
                }else {
                    //??????????????????????????????????????????
                    String msg = (String) r.get("msg");
                    throw new RRException(msg);
                }
            } else{
                //3.2???????????????
                response.setCode(2);
                return response;
            }
        }
    }

    /**
     * ??????????????????
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        //1?????????????????????
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        //2????????????????????????
        orderItemService.saveBatch(order.getOrderItems());
    }


    /**
     * ???????????????????????????(???????????????????????????...)
     * @param seckillOrder
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrder) {
        //1?????????????????????
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setCreateTime(new Date());
        orderEntity.setOrderSn(seckillOrder.getOrderSn());
        orderEntity.setMemberId(seckillOrder.getMemberId());
        orderEntity.setStatus(OrderConstant.OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal multiply = seckillOrder.getSeckillPrice().multiply(new BigDecimal("" + seckillOrder.getNum()));
        orderEntity.setPayAmount(multiply);
        this.save(orderEntity);

        //2????????????????????????
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrder.getOrderSn());
        orderItemEntity.setRealAmount(multiply);
        orderItemEntity.setSkuQuantity(seckillOrder.getNum());
        //????????????SKU???????????????????????????  productFeignService.getSpuInfoBySkuId()???????????????...

        orderItemService.save(orderItemEntity);
    }

    /**
     * ????????????
     * @return
     */
    private OrderCreateTo createOrder(OrderSubmitVo vo) {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1?????????mybatis???IdWorker???????????????
        String orderSn = IdWorker.getTimeId();

        //2???????????????????????????
        OrderEntity order = buildOrder(orderSn,vo);

        //3??????????????????????????????
        List<OrderItemEntity> orderItems = buildOrderItems(orderSn);

        //4??????????????????????????????????????????
        computePrice(order,orderItems);

        orderCreateTo.setOrder(order);
        orderCreateTo.setOrderItems(orderItems);
        orderCreateTo.setPayPrice(order.getPayAmount());
        orderCreateTo.setFare(order.getFreightAmount());


        return orderCreateTo;
    }


    /**
     * ???????????????????????????????????????
     * @param order
     * @param orderItems
     */
    private void computePrice(OrderEntity order, List<OrderItemEntity> orderItems) {
        BigDecimal totalAmount = new BigDecimal("0.0");
        BigDecimal promotionAmount = new BigDecimal("0.0");
        BigDecimal integrationAmount= new BigDecimal("0.0");
        BigDecimal couponAmount = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");

        for (OrderItemEntity orderItem : orderItems) {
            totalAmount = totalAmount.add(orderItem.getRealAmount());
            promotionAmount = promotionAmount.add(orderItem.getPromotionAmount());
            integrationAmount = integrationAmount.add(orderItem.getIntegrationAmount());
            couponAmount = couponAmount.add(orderItem.getCouponAmount());
            integration = integration.add(new BigDecimal(orderItem.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(orderItem.getGiftGrowth().toString()));
        }

        //1???????????????????????????
        order.setTotalAmount(totalAmount);
        //2????????????????????? = ?????? + ??????
        order.setPayAmount(totalAmount.add(order.getFreightAmount()));
        //3???????????????????????????
        order.setPromotionAmount(promotionAmount);
        order.setIntegrationAmount(integrationAmount);
        order.setCouponAmount(couponAmount);
        //4???????????????????????????
        order.setIntegration(integration.intValue());
        order.setGrowth(growth.intValue());
    }


    /**
     * ????????????????????????
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(String orderSn,OrderSubmitVo vo) {
        OrderEntity order = new OrderEntity();

        //1???????????????ware???getFare????????????????????????????????????????????????
        R r = wareFeignService.getFare(vo.getAddrId());
        if(r.getCode() != 0){
            throw new RRException("????????????ware???getFare??????");
        }
        FareVo data = r.getData("data", new TypeReference<FareVo>() {});
        if(data != null){
            //????????????
            order.setFreightAmount(data.getFare());

            //???????????????????????????
            MemberAddressVo address = data.getAddress();
            order.setMemberId(address.getMemberId());
            order.setReceiverName(address.getName());
            order.setReceiverPhone(address.getPhone());
            order.setReceiverProvince(address.getProvince());
            order.setReceiverCity(address.getCity());
            order.setReceiverRegion(address.getRegion());
            order.setReceiverDetailAddress(address.getDetailAddress());
            order.setReceiverPostCode(address.getPostCode());
        }

        //2???????????????????????????
        order.setOrderSn(orderSn);
        order.setCreateTime(new Date());
        order.setPayType(vo.getPayType());
        //???????????????
        order.setStatus(OrderConstant.OrderStatusEnum.CREATE_NEW.getCode());
        order.setAutoConfirmDay(7);
        order.setNote(vo.getNote());
        //??????????????????0???????????????
        order.setDeleteStatus(0);

        return order;
    }

    /**
     * ???????????????????????????
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //1????????????????????????????????????????????????????????????????????????
        //???????????????????????????????????????????????????????????????????????????????????????
        List<OrderItemVo> cartItems = cartFeignService.getCurrentUserCheckedCartItems();
        if(cartItems != null && cartItems.size() > 0){
            List<OrderItemEntity> orderItems = cartItems.stream().map((cartItem) -> {
                //2?????????????????????????????????
                OrderItemEntity orderItem = buildOrderItem(cartItem);
                //3?????????????????????????????????
                orderItem.setOrderSn(orderSn);
                return orderItem;
            }).collect(Collectors.toList());
            return orderItems;
        }
        return null;
    }

    /**
     * ??????????????????????????????
     *
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItem = new OrderItemEntity();

        //1?????????spu??????
        R r = productFeignService.getSpuInfoBySkuId(cartItem.getSkuId());
        if(r.getCode() != 0){
            throw new RRException("????????????product???getSpuInfoBySkuId??????");
        }
        SpuInfoVo spuInfo = r.getData("spuInfo", new TypeReference<SpuInfoVo>() {});
        orderItem.setSpuId(spuInfo.getId());
        orderItem.setSpuName(spuInfo.getSpuName());
        orderItem.setSpuBrand(spuInfo.getBrandId().toString());
        orderItem.setCategoryId(spuInfo.getCatalogId());

        //2?????????sku??????
        orderItem.setSkuId(cartItem.getSkuId());
        orderItem.setSkuName(cartItem.getTitle());
        orderItem.setSkuPic(cartItem.getImage());
        orderItem.setSkuPrice(cartItem.getPrice());
        orderItem.setSkuQuantity(cartItem.getCount());
        String skuAttrs = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        orderItem.setSkuAttrsVals(skuAttrs);
        //3??????????????????????????????

        //4????????????????????????????????????????????????
        orderItem.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItem.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        //5???????????????????????????
        orderItem.setPromotionAmount(new BigDecimal("0"));
        orderItem.setCouponAmount(new BigDecimal("0"));
        orderItem.setIntegrationAmount(new BigDecimal("0"));
        //???????????????????????????????????????-????????????
        BigDecimal origin = orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuQuantity().toString()));
        BigDecimal subtract = origin.subtract(orderItem.getCouponAmount())
                .subtract(orderItem.getPromotionAmount())
                .subtract(orderItem.getIntegrationAmount());
        orderItem.setRealAmount(subtract);


        return orderItem;
    }

    /**
     * ?????????????????????????????????
     * @param orderSn
     * @return
     */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        LambdaQueryWrapper<OrderEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(OrderEntity::getOrderSn,orderSn);
        OrderEntity order = this.getOne(lqw);
        return order;
    }


}