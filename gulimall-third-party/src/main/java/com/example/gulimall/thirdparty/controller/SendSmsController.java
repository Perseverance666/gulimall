package com.example.gulimall.thirdparty.controller;

import com.example.common.utils.R;
import com.example.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Date: 2022/10/17 15:11
 */

@RestController
@RequestMapping("/sms")
public class SendSmsController {

    @Autowired
    private SmsComponent smsComponent;

    /**
     * 发送短信验证码
     * @param phone
     * @param code
     * @return
     */
    @GetMapping("/sendcode")
    public R sendCode(@RequestParam("phone") String phone,@RequestParam("code") String code){
        smsComponent.sendSmsCode(phone,code);
        return R.ok();
    }
}
