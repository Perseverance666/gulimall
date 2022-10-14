package com.example.gulimall.product.web;

import com.example.gulimall.product.service.SkuInfoService;
import com.example.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @Date: 2022/10/14 19:22
 */

@Controller
public class itemController {
    @Autowired
    private SkuInfoService skuInfoService;

    /**
     * 页面商品详情中将指定sku相关的所有信息进行展示
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model){
        SkuItemVo skuItemVo = skuInfoService.item(skuId);
//        model.addAttribute();
        return "item";
    }
}
