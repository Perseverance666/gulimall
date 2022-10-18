package com.example.gulimall.auth.controller;


import com.example.common.constant.AuthConstant;
import com.example.common.exception.BizCodeEnum;
import com.example.common.utils.R;
import com.example.gulimall.auth.feign.MemberFeignService;
import com.example.gulimall.auth.feign.ThirdPartyFeignService;
import com.example.gulimall.auth.vo.UserLoginVo;
import com.example.gulimall.auth.vo.UserRegisterVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Date: 2022/10/16 19:54
 */

@Controller
public class AuthController {

    @Autowired
    private ThirdPartyFeignService thirdPartyFeignService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private MemberFeignService memberFeignService;

    /**
     * 获取短信验证码
     * @param phone
     * @return
     */
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){

        //TODO 1、接口防刷

        //2、对redis中存放的code进行检验，检验是否已过60秒，60秒内不能再发
        String redisCode = redisTemplate.opsForValue().get(AuthConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(StringUtils.isNotEmpty(redisCode)){
            long time = Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis() - time < 60000){
                //60秒内不能再发
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(),BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }


        //3、验证码存入redis。存key -> sms:code:phone   value -> code_System.currentTimeMillis()
        String code = UUID.randomUUID().toString().substring(0, 5);
        String substring = code + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(AuthConstant.SMS_CODE_CACHE_PREFIX+phone,substring,5, TimeUnit.MINUTES);
        thirdPartyFeignService.sendCode(phone,code);
        return R.ok();
    }

    /**
     * 注册功能
     *
     *   //TODO 重定向携带数据，利用session原理。将数据放在session中。
     *     只要跳到下一个页面取出这个数据以后，session里面的数据就会删掉
     *
     *  //TODO 分布式下的session问题。
     * @param vo
     * @param result
     * @param redirectAttributes 模拟重定向携带数据
     * @return
     */
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo vo, BindingResult result,
                           RedirectAttributes redirectAttributes){
        //1、vo格式校验出错，转发到注册页，将错误存入redirectAttributes
        if(result.hasErrors()){
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            //model.addAttribute("errors",errors);
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }

        //2、校验验证码
        String code = vo.getCode();
        String redisCode = redisTemplate.opsForValue().get(AuthConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        //2.1、redis中验证码已过期
        if(StringUtils.isEmpty(redisCode)){
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码已过期");
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
        //2.2、验证码错误
        if(!code.equals(redisCode.split("_")[0])){
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码已过期");
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
        //2.3、验证码正确。删除验证码-令牌机制
        redisTemplate.delete(AuthConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());

        //3、远程调用，进行注册
        R r = memberFeignService.register(vo);
        if(r.getCode() == 0){
            //3.1、注册成功，回到登录页
            return "redirect:http://auth.gulimall.com/login.html";
        }else{
            //3.2、注册失败，返回失败原因
            Map<String, String> errors = new HashMap<>();
            errors.put("msg",r.getMsg());
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
    }

    /**
     * 注册会员功能
     * @param vo
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/login")
    public String login(UserLoginVo vo,RedirectAttributes redirectAttributes){
        R r = memberFeignService.login(vo);
        if(r.getCode() == 0){
            return "redirect:http://gulimall.com";
        }else{
            Map<String, String> errors = new HashMap<>();
            errors.put("msg",r.getMsg());
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }


}
