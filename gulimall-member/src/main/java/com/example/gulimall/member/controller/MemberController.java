package com.example.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.example.common.exception.BizCodeEnum;
import com.example.gulimall.member.exception.PhoneExistException;
import com.example.gulimall.member.exception.UserNameExistException;
import com.example.gulimall.member.vo.MemberLoginVo;
import com.example.gulimall.member.vo.MemberRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.gulimall.member.entity.MemberEntity;
import com.example.gulimall.member.service.MemberService;
import com.example.common.utils.PageUtils;
import com.example.common.utils.R;



/**
 * 会员
 *
 * @author ll
 * @email ll@gmail.com
 * @date 2022-08-13 14:04:02
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    /**
     * 会员登录功能
     * @param vo
     * @return
     */
    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo){
        MemberEntity memberEntity = memberService.login(vo);
        if(memberEntity != null){
            return R.ok();
        }else{
            return R.error(BizCodeEnum.ACCOUNT_PASSWORD_INVALID_EXCEPTION.getCode(),BizCodeEnum.ACCOUNT_PASSWORD_INVALID_EXCEPTION.getMsg());
        }
    }

    /**
     * 注册会员功能
     * @param vo
     * @return
     */
    @PostMapping("/register")
    public R register(@RequestBody MemberRegisterVo vo){
        try{
            memberService.register(vo);
        }catch (PhoneExistException e){
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(),BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        }catch (UserNameExistException e){
            return R.error(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(),BizCodeEnum.USER_EXIST_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
