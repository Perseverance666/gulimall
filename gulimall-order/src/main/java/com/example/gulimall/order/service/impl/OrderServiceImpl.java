package com.example.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.common.constant.OrderConstant;
import com.example.common.utils.R;
import com.example.common.vo.MemberRespVo;
import com.example.gulimall.order.entity.OrderItemEntity;
import com.example.gulimall.order.feign.CartFeignService;
import com.example.gulimall.order.feign.MemberFeignService;
import com.example.gulimall.order.feign.WareFeignService;
import com.example.gulimall.order.interceptor.LoginInterceptor;
import com.example.gulimall.order.to.OrderCreateTo;
import com.example.gulimall.order.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
     * @param vo
     * @return
     */
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
            OrderCreateTo createTo = createOrder();
            return response;
        }

    }

    /**
     * 创建订单
     * @return
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1、使用mybatis的IdWorker生成订单号
        String orderSn = IdWorker.getTimeId();

        //2、构建订单基本信息
        OrderEntity orderEntity = buildOrder(orderSn);

        //3、构建所有订单项信息
        List<OrderItemEntity> orderItemEntities = buildOrderItems();

        //4、验价

        return orderCreateTo;
    }


    /**
     * 构建订单基本信息
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(String orderSn) {
        return null;
    }

    /**
     * 构建所有订单项信息
     * @return
     */
    private List<OrderItemEntity> buildOrderItems() {
        return null;
    }

    /**
     * 构建某一个订单项信息
     *
     * @return
     */
    private OrderItemEntity buildOrderItem() {
        return null;
    }



}