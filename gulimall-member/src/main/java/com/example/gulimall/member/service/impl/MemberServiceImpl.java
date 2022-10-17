package com.example.gulimall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gulimall.member.entity.MemberLevelEntity;
import com.example.gulimall.member.exception.PhoneExistException;
import com.example.gulimall.member.exception.UserNameExistException;
import com.example.gulimall.member.service.MemberLevelService;
import com.example.gulimall.member.vo.MemberLoginVo;
import com.example.gulimall.member.vo.MemberRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
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