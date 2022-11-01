package com.example.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.common.constant.OrderConstant;
import com.example.common.exception.RRException;
import com.example.common.to.mq.OrderTo;
import com.example.common.utils.R;
import com.example.common.vo.MemberRespVo;
import com.example.gulimall.order.entity.OrderItemEntity;
import com.example.gulimall.order.feign.CartFeignService;
import com.example.gulimall.order.feign.MemberFeignService;
import com.example.gulimall.order.feign.ProductFeignService;
import com.example.gulimall.order.feign.WareFeignService;
import com.example.gulimall.order.interceptor.LoginInterceptor;
import com.example.gulimall.order.service.OrderItemService;
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

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 分页查询当前登录用户的所有订单及订单项信息
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
     * 根据订单号，查询订单支付信息
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPayByOrderSn(String orderSn) {
        PayVo payVo = new PayVo();
        //设置商户订单号
        payVo.setOut_trade_no(orderSn);

        OrderEntity order = this.getOrderByOrderSn(orderSn);
        BigDecimal payAmount = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        //设置付款金额
        payVo.setTotal_amount(payAmount.toString());

        List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = order_sn.get(0);
        //将第一个商品设置为订单标题
        payVo.setSubject(entity.getSkuName());

        //将第一个商品的销售属性设置为商品描述
        payVo.setBody(entity.getSkuAttrsVals());
        return payVo;
    }


    /**
     * 关闭订单
     *
     * 关闭订单后，可能由于关闭订单卡顿等问题，导致先解锁库存再关闭订单，此时由于订单还是待付款状态，无法解锁库存，
     * 等到关闭订单后，这个库存就永远无法被解锁。
     * 解决方法：在关闭订单后，发送消息，去进行解锁库存。 双重解锁库存确保安全
     * @param order
     */
    @Override
    public void closeOrder(OrderEntity order) {
        //只有订单状态是待付款才能关闭
        if(order.getStatus() == OrderConstant.OrderStatusEnum.CREATE_NEW.getCode()){
            //1、关闭订单
            order.setStatus(OrderConstant.OrderStatusEnum.CANCLED.getCode());
            this.updateById(order);
            //2、关闭订单后，要发送消息，去进行解锁库存
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(order,orderTo);
            try {
                //TODO 保证消息一定会发送出去，每一个消息都可以做好日志记录（给数据库保存每一个消息的详细信息）。
                //TODO 定期扫描数据库将失败的消息再发送一遍；
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            }catch (Exception e){
                //TODO 将没法送成功的消息进行重试发送。
            }
        }
    }

    /**
     * 订单确认页返回需要的数据
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberRespVo loginUser = LoginInterceptor.loginUser.get();
        System.out.println("主线程...." + Thread.currentThread().getId());
        //获取之前的请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        //1、远程调用查询会员所有的收货地址
        CompletableFuture<Void> memberFuture = CompletableFuture.runAsync(() -> {
            System.out.println("member线程...." + Thread.currentThread().getId());
            //异步情况下，让每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);

            List<MemberAddressVo> addresses = memberFeignService.getAddresses(loginUser.getId());
            orderConfirmVo.setAddress(addresses);
        }, executor);

        //2、远程调用查询购物车中所有被选中的购物项。Feign远程调用会丢失请求头
        CompletableFuture<Void> cartFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("cart线程...." + Thread.currentThread().getId());
            //异步情况下，让每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);

            List<OrderItemVo> items = cartFeignService.getCurrentUserCheckedCartItems();
            orderConfirmVo.setItems(items);
            return items;
        }, executor).thenAcceptAsync((items) -> {
            //2.1、异步查询完购物项信息后，远程调用查询购物项是否有库存
            List<Long> skuIds = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R hasStock = wareFeignService.getSkusHasStock(skuIds);
            List<SkuStockVo> data = hasStock.getData("data", new TypeReference<List<SkuStockVo>>() {});
            if(data != null){
                Map<Long, Boolean> stocks = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                //2.2、设置orderConfirmVo的库存信息
                orderConfirmVo.setStocks(stocks);
            }
        },executor);

        //3、查询用户积分信息
        orderConfirmVo.setIntegration(loginUser.getIntegration());

        //4、其他价格数据自动计算

        //5、订单防重令牌
        String orderToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId(),orderToken);
        orderConfirmVo.setOrderToken(orderToken);

        CompletableFuture.allOf(memberFuture,cartFuture).get();
        return orderConfirmVo;
    }

    /**
     * 下单功能
     *
     * 1、验证令牌 -> 创建订单 -> 验价(可选) -> 保存订单数据 -> 锁库存
     * 2、锁定库存成功后，发消息给mq告诉订单创建成功(消息进入order.delay.queue)
     * -> 经过1分钟订单过期(消息进入order.release.order.queue)
     * -> orderCloseListener监听到消息，开始关闭订单
     * -> 关闭订单后，发消息给mq告诉解锁库存(消息进入stock.release.stock.queue)
     * -> handleOrderCloseRelease监听到消息，由于订单关闭，开始解锁库存
     *
     * 事务概念：
     * 1、本地事务，在分布式系统，只能控制住自己的回滚，控制不了其他服务的回滚
     * 2、分布式事务： 最大原因：网络问题+分布式机器。
     * @param vo
     * @return
     */
//    @GlobalTransactional    //高并发不适合
    @Transactional
    @Override
    public SubmitOrderRespVo submitOrder(OrderSubmitVo vo) {
        SubmitOrderRespVo response = new SubmitOrderRespVo();
        MemberRespVo loginUser = LoginInterceptor.loginUser.get();
        //1、首先验证令牌token
        //1.1、使用lua脚本来获取令牌和删除令牌，保证原子性。
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        Long result = redisTemplate.execute(new DefaultRedisScript<>(luaScript, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId()), orderToken);
        if (result == 0L){
            //1.2、令牌验证失败
            response.setCode(1);
            return response;
        }else{
            //1.3、令牌验证成功

            //2、创建订单
            OrderCreateTo order = createOrder(vo);

            //3、验价
            BigDecimal payAmount = order.getPayPrice();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //3.1、验价成功

                //4、保存订单数据
                saveOrder(order);

                //5、锁库存。若出异常，则回滚，不保存订单数据
                //5.1、封装锁库存所需要的数据
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

                //5.2、远程调用ware的orderLockStock，进行锁库存
                //库存成功了，但是网络原因超时了，订单回滚，库存不滚。
                //为了保证高并发。库存服务自己回滚。可以发消息给库存服务；
                //库存服务本身也可以使用自动解锁模式  消息
                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if(r.getCode() == 0){
                    //锁库存成功
                    response.setOrder(order.getOrder());
                    response.setCode(0);
//                    int i = 10/0; //模拟异常，订单回滚，库存不滚
                    //6、给mq发送消息，告诉订单创建成功
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    return response;
                }else {
                    //锁库存失败，抛异常，进行回滚
                    String msg = (String) r.get("msg");
                    throw new RRException(msg);
                }
            } else{
                //3.2、验价失败
                response.setCode(2);
                return response;
            }
        }
    }

    /**
     * 保存订单数据
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        //1、保存订单数据
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        //2、保存订单项数据
        orderItemService.saveBatch(order.getOrderItems());
    }

    /**
     * 创建订单
     * @return
     */
    private OrderCreateTo createOrder(OrderSubmitVo vo) {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1、使用mybatis的IdWorker生成订单号
        String orderSn = IdWorker.getTimeId();

        //2、构建订单基本信息
        OrderEntity order = buildOrder(orderSn,vo);

        //3、构建所有订单项信息
        List<OrderItemEntity> orderItems = buildOrderItems(orderSn);

        //4、构建订单的金额，积分等信息
        computePrice(order,orderItems);

        orderCreateTo.setOrder(order);
        orderCreateTo.setOrderItems(orderItems);
        orderCreateTo.setPayPrice(order.getPayAmount());
        orderCreateTo.setFare(order.getFreightAmount());


        return orderCreateTo;
    }

    /**
     * 构建订单的金额，积分等信息
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

        //1、设置订单总价金额
        order.setTotalAmount(totalAmount);
        //2、设置应付价格 = 总价 + 运费
        order.setPayAmount(totalAmount.add(order.getFreightAmount()));
        //3、设置各种促销金额
        order.setPromotionAmount(promotionAmount);
        order.setIntegrationAmount(integrationAmount);
        order.setCouponAmount(couponAmount);
        //4、设置订单积分信息
        order.setIntegration(integration.intValue());
        order.setGrowth(growth.intValue());
    }


    /**
     * 构建订单基本信息
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(String orderSn,OrderSubmitVo vo) {
        OrderEntity order = new OrderEntity();

        //1、远程调用ware的getFare方法，返回的是运费和会员地址信息
        R r = wareFeignService.getFare(vo.getAddrId());
        if(r.getCode() != 0){
            throw new RRException("远程调用ware的getFare失败");
        }
        FareVo data = r.getData("data", new TypeReference<FareVo>() {});
        if(data != null){
            //设置运费
            order.setFreightAmount(data.getFare());

            //设置订单收货人信息
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

        //2、设置订单其他信息
        order.setOrderSn(orderSn);
        order.setCreateTime(new Date());
        order.setPayType(vo.getPayType());
        //待付款状态
        order.setStatus(OrderConstant.OrderStatusEnum.CREATE_NEW.getCode());
        order.setAutoConfirmDay(7);
        order.setNote(vo.getNote());
        //设置删除状态0代表未删除
        order.setDeleteStatus(0);

        return order;
    }

    /**
     * 构建所有订单项信息
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //1、远程调用，获取当前用户购物车中被选中购物项信息
        //最后确定每个购物项的价格，后台调价，订单中的价格也不会改变
        List<OrderItemVo> cartItems = cartFeignService.getCurrentUserCheckedCartItems();
        if(cartItems != null && cartItems.size() > 0){
            List<OrderItemEntity> orderItems = cartItems.stream().map((cartItem) -> {
                //2、构建某一个订单项信息
                OrderItemEntity orderItem = buildOrderItem(cartItem);
                //3、为该订单项设置订单号
                orderItem.setOrderSn(orderSn);
                return orderItem;
            }).collect(Collectors.toList());
            return orderItems;
        }
        return null;
    }

    /**
     * 构建某一个订单项信息
     *
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItem = new OrderItemEntity();

        //1、设置spu信息
        R r = productFeignService.getSpuInfoBySkuId(cartItem.getSkuId());
        if(r.getCode() != 0){
            throw new RRException("远程调用product的getSpuInfoBySkuId失败");
        }
        SpuInfoVo spuInfo = r.getData("spuInfo", new TypeReference<SpuInfoVo>() {});
        orderItem.setSpuId(spuInfo.getId());
        orderItem.setSpuName(spuInfo.getSpuName());
        orderItem.setSpuBrand(spuInfo.getBrandId().toString());
        orderItem.setCategoryId(spuInfo.getCatalogId());

        //2、设置sku信息
        orderItem.setSkuId(cartItem.getSkuId());
        orderItem.setSkuName(cartItem.getTitle());
        orderItem.setSkuPic(cartItem.getImage());
        orderItem.setSkuPrice(cartItem.getPrice());
        orderItem.setSkuQuantity(cartItem.getCount());
        String skuAttrs = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        orderItem.setSkuAttrsVals(skuAttrs);
        //3、优惠信息（忽略了）

        //4、设置积分信息（使用价格来模拟）
        orderItem.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItem.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        //5、订单项的价格信息
        orderItem.setPromotionAmount(new BigDecimal("0"));
        orderItem.setCouponAmount(new BigDecimal("0"));
        orderItem.setIntegrationAmount(new BigDecimal("0"));
        //当前订单项的实际金额：总额-各种优惠
        BigDecimal origin = orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuQuantity().toString()));
        BigDecimal subtract = origin.subtract(orderItem.getCouponAmount())
                .subtract(orderItem.getPromotionAmount())
                .subtract(orderItem.getIntegrationAmount());
        orderItem.setRealAmount(subtract);


        return orderItem;
    }

    /**
     * 根据订单号查询订单信息
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