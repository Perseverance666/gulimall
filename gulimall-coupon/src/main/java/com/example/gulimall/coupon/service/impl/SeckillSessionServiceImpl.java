package com.example.gulimall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.example.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.coupon.dao.SeckillSessionDao;
import com.example.gulimall.coupon.entity.SeckillSessionEntity;
import com.example.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    private SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取未来三天的秒杀场次 (从今天0时开始算)
     *
     * 只需session的start_time在区间 [nowDate 00:00:00 , nowDate+2 23:59:59]上
     * 例:[2022-11-04 00:00:00 , 2022-11-06 23:59:59]
     * @return
     */
    @Override
    public List<SeckillSessionEntity> getSeckillSessionLatest3Days() {
        LambdaQueryWrapper<SeckillSessionEntity> lqw = new LambdaQueryWrapper<>();
        //1、计算开始时间
        String startTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        //2、计算3天后的结束时间
        String endTime = LocalDateTime.of(LocalDate.now().plusDays(2), LocalTime.MAX).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        lqw.between(SeckillSessionEntity::getStartTime,startTime,endTime);
        //3、获取未来三天的秒杀场次
        List<SeckillSessionEntity> sessions = this.list(lqw);
        if(sessions == null || sessions.size() == 0){
            return null;
        }

        //4、在秒杀场次中封装该场次的秒杀商品
        List<SeckillSessionEntity> collect = sessions.stream().map(session -> {
            Long sessionId = session.getId();
            LambdaQueryWrapper<SeckillSkuRelationEntity> lqw2 = new LambdaQueryWrapper<>();
            lqw2.eq(SeckillSkuRelationEntity::getPromotionSessionId, sessionId);
            List<SeckillSkuRelationEntity> relationSkus = seckillSkuRelationService.list(lqw2);
            session.setRelationSkus(relationSkus);
            return session;
        }).collect(Collectors.toList());

        return collect;
    }


}