package com.example.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.common.constant.AuthConstant;
import com.example.common.utils.HttpUtils;
import com.example.common.utils.R;
import com.example.gulimall.auth.config.OAuth2Component;
import com.example.gulimall.auth.feign.MemberFeignService;
import com.example.common.vo.MemberRespVo;
import com.example.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @Date: 2022/10/18 17:08
 * 社交登录成功回调
 */

@Slf4j
@Controller
@RequestMapping("/oauth2.0")
public class OAuth2Controller {
    @Autowired
    private OAuth2Component component;
    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/gitee/success")
    public String giteeSuccess(@RequestParam("code") String code, HttpSession session) throws Exception {
        Map<String,String> header = new HashMap<>();
        Map<String,String> query = new HashMap<>();

        Map<String,String> map = new HashMap<>();
        map.put("grant_type",component.getGrantType());
        map.put("code",code);
        map.put("client_id",component.getClientId());
        map.put("redirect_uri",component.getRedirectUri());
        map.put("client_secret",component.getClientSecret());

        //换取accessToken
        HttpResponse response = HttpUtils.doPost("https://gitee.com", "/oauth/token", "post", header,query, map);
        if (response.getStatusLine().getStatusCode() == 200){
            //获取accessToken成功
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            log.info("accessToken:{}",socialUser.getAccess_token());
            R r = memberFeignService.oauthLogin(socialUser);
            if(r.getCode() == 0){
                //登录成功，进入首页
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {});
                log.info("登录成功：用户：{}",data.toString());
                //用户信息存入session，SpringSession将数据存入redis
                session.setAttribute(AuthConstant.LOGIN_USER,data);

                return "redirect:http://gulimall.com";
            }else{
                //登录失败
                return "redirect:http://auth.gulimall.com/login.html";
            }

        }else {
            //获取accessToken失败
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
