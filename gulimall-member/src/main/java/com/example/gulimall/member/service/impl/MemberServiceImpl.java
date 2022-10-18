package com.example.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.utils.HttpUtils;
import com.example.gulimall.member.entity.MemberLevelEntity;
import com.example.gulimall.member.exception.PhoneExistException;
import com.example.gulimall.member.exception.UserNameExistException;
import com.example.gulimall.member.service.MemberLevelService;
import com.example.gulimall.member.vo.MemberLoginVo;
import com.example.gulimall.member.vo.MemberRegisterVo;
import com.example.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.member.dao.MemberDao;
import com.example.gulimall.member.entity.MemberEntity;
import com.example.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelService memberLevelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 社交用户登录功能
     * @param socialUser
     * @return
     */
    @Override
    public MemberEntity oauthLogin(SocialUser socialUser) throws Exception {
        //1、获取社交用户的uid。gitee跟weibo不一样，需要发送get请求才能查到uid
        Map<String,String> queryMap = new HashMap<>();
        queryMap.put("access_token",socialUser.getAccess_token());
        HttpResponse response = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", new HashMap<String, String>(), queryMap);
        if(response.getStatusLine().getStatusCode() == 200){
            String json = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = JSON.parseObject(json);
            String uid = jsonObject.getString("id");
            //2、去ums_member表中查询，查看该用户是否注册
            LambdaQueryWrapper<MemberEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(MemberEntity::getSocialUid,uid);
            MemberEntity memberEntity = this.baseMapper.selectOne(lqw);
            if(memberEntity != null){
                //2.1、该社交用户已经注册
                return memberEntity;
            }else{
                //2.2、该社交用户未注册，进行注册
                MemberEntity newMember = new MemberEntity();
                newMember.setUsername(jsonObject.getString("login"));
                newMember.setNickname(jsonObject.getString("name"));
                newMember.setCreateTime(new Date());
                newMember.setLevelId(1L);
                newMember.setSocialUid(uid);
                newMember.setSocialType("gitee");

                this.baseMapper.insert(newMember);

                return newMember;
            }

        }else{
            //查询社交用户信息出现异常
            return null;
        }
    }

    /**
     * 会员登录功能
     * @param vo
     * @return
     */
    @Override
    public MemberEntity login(MemberLoginVo vo) {
        LambdaQueryWrapper<MemberEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(MemberEntity::getUsername,vo.getAccount()).or().eq(MemberEntity::getMobile,vo.getAccount());
        MemberEntity memberEntity = this.getOne(lqw);
        if(memberEntity == null){
            //没有该用户名或者手机号
            return null;
        }else{
            //校验密码
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            boolean matches = passwordEncoder.matches(vo.getPassword(), memberEntity.getPassword());
            if(matches){
                //密码匹配
                return memberEntity;
            }else{
                //密码不匹配
                return null;
            }

        }

    }


    /**
     * 注册会员功能
     * @param vo
     */
    @Override
    public void register(MemberRegisterVo vo) {
        MemberEntity memberEntity = new MemberEntity();

        //1.检查用户名和手机号是否唯一。为了让controller能感知异常，异常机制
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUsername());

        memberEntity.setMobile(vo.getPhone());
        memberEntity.setUsername(vo.getUsername());

        memberEntity.setNickname(vo.getUsername());

        //2.设置默认等级
        MemberLevelEntity memberLevelEntity = memberLevelService.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());

        //3.密码要进行加密存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);

        //4、其他的默认信息

        this.baseMapper.insert(memberEntity);
    }

    /**
     * 检验username是否存在
     * @param username
     */
    @Override
    public void checkUsernameUnique (String username) throws UserNameExistException{
        LambdaQueryWrapper<MemberEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(username != null,MemberEntity::getUsername,username);
        Integer count = this.baseMapper.selectCount(lqw);
        if(count > 0){
            throw new UserNameExistException();
        }
    }

    /**
     * 检验phone是否存在
     * @param phone
     */
    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException{
        LambdaQueryWrapper<MemberEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(phone != null,MemberEntity::getMobile,phone);
        Integer count = this.baseMapper.selectCount(lqw);
        if(count > 0){
            throw new PhoneExistException();
        }
    }




}