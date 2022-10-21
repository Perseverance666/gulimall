package com.example.gulimall.search.controller;

import com.example.gulimall.search.service.SearchService;
import com.example.gulimall.search.vo.SearchParam;
import com.example.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @Date: 2022/10/10 17:11
 */

@Controller
public class SearchController {
    @Autowired
    private SearchService searchService;

    /**
     * 自动将页面提交过来的所有请求查询参数封装成SearchParam对象，然后再到es中去查询，返回SearchResult结果，并展示到页面上
     * @param param
     * @return
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request){
        //设置原生的所有查询条件
        param.set_queryString(request.getQueryString());
        SearchResult result = searchService.search(param);
        model.addAttribute("result",result);

        return "list";
    }
}
