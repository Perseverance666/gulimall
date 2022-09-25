package com.example.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * spu信息介绍
 * 
 * @author liu
 * @email liu@gmail.com
 * @date 2022-09-16 13:25:00
 */
@Data
@TableName("pms_spu_info_desc")
public class SpuInfoDescEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 商品id
	 */
	@TableId(type = IdType.INPUT)		//自己输入的主键，不是默认自增的
	private Long spuId;
	/**
	 * 商品介绍
	 */
	private String decript;

}
