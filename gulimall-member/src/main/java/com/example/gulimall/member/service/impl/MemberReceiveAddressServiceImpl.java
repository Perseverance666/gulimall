package com.example.gulimall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.member.dao.MemberReceiveAddressDao;
import com.example.gulimall.member.entity.MemberReceiveAddressEntity;
import com.example.gulimall.member.service.MemberReceiveAddressService;


@Service("memberReceiveAddressService")
public class MemberReceiveAddressServiceImpl extends ServiceImpl<MemberReceiveAddressDao, MemberReceiveAddressEntity> implements MemberReceiveAddressService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberReceiveAddressEntity> page = this.page(
                new Query<MemberReceiveAddressEntity>().getPage(params),
                new QueryWrapper<MemberReceiveAddressEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取会员的所有地址信息
     * @param memberId
     * @return
     */
    @Override
    public List<MemberReceiveAddressEntity> getAddresses(Long memberId) {
        LambdaQueryWrapper<MemberReceiveAddressEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(memberId != null,MemberReceiveAddressEntity::getMemberId,memberId);
        return this.list(lqw);
    }

}