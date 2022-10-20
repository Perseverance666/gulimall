package com.atguigu.gulimall.ssoserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;


@Controller
public class LoginController {


    @Autowired
    StringRedisTemplate redisTemplate;

    @ResponseBody
    @GetMapping("/userInfo")
    public String userInfo(@RequestParam("token") String token){
        String s = redisTemplate.opsForValue().get(token);
        return s;
    }

    /**
     * 来到登录页
     * @param url
     * @param model
     * @param sso_token
     * @return
     */
    @GetMapping("/login.html")
    public String loginPage(@RequestParam("redirect_url") String url, Model model,
                            @CookieValue(value = "sso_token",required = false) String sso_token){
        if(!StringUtils.isEmpty(sso_token)){
            //说明之前有人登录过，浏览器留下了痕迹（cookie中存放了token）
            return "redirect:"+url+"?token="+sso_token;
        }

        model.addAttribute("url",url);
        return "login";
    }

    /**
     * 登录功能
     * @param username
     * @param password
     * @param url
     * @param response
     * @return
     */
    @PostMapping("/doLogin")
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password")String password,
                          @RequestParam("url")String url,
                          HttpServletResponse response){

        if(!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)){
            //把登录成功的用户存入session（session将数据存入redis，这里直接存redis来模拟）。key->token,value->用户信息 。
            String uuid = UUID.randomUUID().toString().replace("-","");
            redisTemplate.opsForValue().set(uuid,username);

            //给浏览器留下痕迹，cookie，说明有人已经登录过了
            Cookie sso_token = new Cookie("sso_token",uuid);
            response.addCookie(sso_token);

            //登录成功，跳回之前页面，并带上token
            return "redirect:"+url+"?token="+uuid;
        }
        //登录失败，展示登录页
        return "login";
    }
}
