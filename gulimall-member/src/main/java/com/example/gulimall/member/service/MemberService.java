package com.example.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.utils.PageUtils;
import com.example.gulimall.member.entity.MemberEntity;
import com.example.gulimall.member.exception.PhoneExistException;
import com.example.gulimall.member.exception.UserNameExistException;
import com.example.gulimall.member.vo.MemberLoginVo;
import com.example.gulimall.member.vo.MemberRegisterVo;

import java.util.Map;

/**
 * 会员
 *
 * @author ll
 * @email ll@gmail.com
 * @date 2022-08-13 14:04:02
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberRegisterVo vo);

    void checkUsernameUnique(String username) throws UserNameExistException;

    void checkPhoneUnique(String phone) throws PhoneExistException;

    MemberEntity login(MemberLoginVo vo);
}

