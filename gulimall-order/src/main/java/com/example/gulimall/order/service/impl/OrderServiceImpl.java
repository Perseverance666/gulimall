package com.example.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.common.constant.CartConstant;
import com.example.common.constant.OrderConstant;
import com.example.common.exception.RRException;
import com.example.common.utils.R;
import com.example.common.vo.MemberRespVo;
import com.example.gulimall.order.entity.OrderItemEntity;
import com.example.gulimall.order.enume.OrderStatusEnum;
import com.example.gulimall.order.feign.CartFeignService;
import com.example.gulimall.order.feign.MemberFeignService;
import com.example.gulimall.order.feign.ProductFeignService;
import com.example.gulimall.order.feign.WareFeignService;
import com.example.gulimall.order.interceptor.LoginInterceptor;
import com.example.gulimall.order.service.OrderItemService;
import com.example.gulimall.order.to.OrderCreateTo;
import com.example.gulimall.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
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

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
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
     * 验证令牌 -> 创建订单 -> 验价(可选) -> 保存订单数据 -> 锁库存
     *
     * 本地事务，在分布式系统，只能控制住自己的回滚，控制不了其他服务的回滚
     * 分布式事务： 最大原因。网络问题+分布式机器。
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

                    int i = 10/0; //模拟异常，订单回滚，库存不滚
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
        order.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
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



}