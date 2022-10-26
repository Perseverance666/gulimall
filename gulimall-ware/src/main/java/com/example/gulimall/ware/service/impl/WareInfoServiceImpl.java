package com.example.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.utils.R;
import com.example.gulimall.ware.feign.MemberFeignService;
import com.example.gulimall.ware.vo.FareVo;
import com.example.gulimall.ware.vo.MemberAddressVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.ware.dao.WareInfoDao;
import com.example.gulimall.ware.entity.WareInfoEntity;
import com.example.gulimall.ware.service.WareInfoService;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    private MemberFeignService memberFeignService;

    /**
     * 仓库列表
     * 库存系统，仓库维护，列表展示
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        LambdaQueryWrapper<WareInfoEntity> lqw = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");
        if(StringUtils.isNotEmpty(key)){
            lqw.eq(WareInfoEntity::getId,key)
                    .or().like(WareInfoEntity::getName,key)
                    .or().like(WareInfoEntity::getAddress,key)
                    .or().like(WareInfoEntity::getAreacode,key);
        }

        IPage<WareInfoEntity> page = this.page(new Query<WareInfoEntity>().getPage(params),lqw);
        return new PageUtils(page);
    }

    /**
     * 根据地址信息，计算运费
     * @param addrId
     * @return
     */
    @Override
    public FareVo getFare(Long addrId) {
        //1、远程调用查询会员地址信息
        R r = memberFeignService.getAddrinfo(addrId);
        MemberAddressVo vo = r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {});
        if (vo== null){
            return null;
        }
        //2、省略调用第三方接口来计算运费，使用手机号最后一位，来模拟运费价格
        String phone = vo.getPhone();
        String substring = phone.substring(phone.length() - 1, phone.length());
        BigDecimal fare = new BigDecimal(substring);

        //3、封装fareVo
        FareVo fareVo = new FareVo();
        fareVo.setAddress(vo);
        fareVo.setFare(fare);
        return fareVo;
    }

}