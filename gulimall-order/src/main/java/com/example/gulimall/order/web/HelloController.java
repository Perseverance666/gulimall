package com.example.gulimall.order.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @Date: 2022/10/25 13:48
 */

@Controller
public class HelloController {

    @GetMapping("/{page}.html")
    public String listPage(@PathVariable String page){
        return page;
    }
}
