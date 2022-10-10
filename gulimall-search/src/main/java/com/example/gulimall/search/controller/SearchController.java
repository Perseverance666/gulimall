package com.example.gulimall.search.controller;

import com.example.gulimall.search.service.MallSearchService;
import com.example.gulimall.search.vo.SearchParam;
import com.example.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Date: 2022/10/10 17:11
 */

@Controller
public class SearchController {
    @Autowired
    private MallSearchService mallSearchService;

    /**
     * 自动将页面提交过来的所有请求查询参数封装成SearchParam对象，然后再到es中去查询，返回SearchResult结果，并展示到页面上
     * @param param
     * @return
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model){
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result",result);

        return "list";
    }
}
