package com.example.gulimall.member.dao;

import com.example.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author ll
 * @email ll@gmail.com
 * @date 2022-08-13 14:04:02
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
