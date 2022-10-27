package com.example.gulimall.cart.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;


/**
 * @Date: 2022/10/21 15:46
 * 购物项内容
 */

public class CartItem {

    @Getter @Setter
    private Long skuId;

    @Getter @Setter
    private Boolean check = true;       //默认被选中

    @Getter @Setter
    private String title;

    @Getter @Setter
    private String image;

    @Getter @Setter
    private List<String> skuAttrs;

    @Getter @Setter
    private BigDecimal price;

    @Getter @Setter
    private Integer count;  //商品数量

    @Setter
    private BigDecimal totalPrice;      //小计

    /**
     * 计算当前项的总价，小计
     * @return
     */
    public BigDecimal getTotalPrice() {
        return this.price.multiply(new BigDecimal(this.count.toString()));
    }
}
