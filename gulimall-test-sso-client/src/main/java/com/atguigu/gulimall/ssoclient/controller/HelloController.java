package com.atguigu.gulimall.ssoclient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HelloController {


    @Value("${sso.server.url}")
    String ssoServerUrl;


    /**
     * 无需登录就可访问
     * @return
     */
    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }


    /**
     * 感知这次是在 ssoserver 登录成功跳回来的。
     * @param model
     * @param session
     * @param token 只要去ssoserver登录成功,跳回来时就会带上
     * @return
     */
    @GetMapping("/employees")
    public String employees(Model model, HttpSession session,
                            @RequestParam(value = "token",required = false) String token){
        //去ssoserver登录成功跳回来就会带上token
        if(!StringUtils.isEmpty(token)){
            //用spring提供的RestTemplate来发get请求
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> forEntity = restTemplate.getForEntity("http://ssoserver.com:8080/userInfo?token=" + token, String.class);
            String body = forEntity.getBody();
            session.setAttribute("loginUser",body);
        }

        Object loginUser = session.getAttribute("loginUser");
        if(loginUser==null){
            //没登录，跳转到登录服务器进行登录，并携带redirect_url，来标识我们自己是哪个页面
            return "redirect:"+ssoServerUrl+"?redirect_url=http://client1.com:8081/employees";
        }else{
            List<String> emps = new ArrayList<>();
            emps.add("张三");
            emps.add("李四");

            model.addAttribute("emps",emps);
            return "list";
        }


    }



}
