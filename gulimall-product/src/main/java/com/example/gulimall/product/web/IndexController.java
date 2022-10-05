package com.example.gulimall.product.web;

import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.CategoryService;
import com.example.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @Date: 2022/10/5 16:36
 */

@Controller
public class IndexController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 将一级分类展示到页面中
     * @param model
     * @return
     */
    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){
        //查询所有一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categories();

        //视图解析器进行拼串
        // classpath:/templates/+ 返回值 + .html
        model.addAttribute("categories",categoryEntities);
        return "index";
    }

    /**
     * /index/catalog.json
     * 鼠标停放在一级分类时，显示对应的2级和3级分类
     * @return
     */
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String,List<Catelog2Vo>>  getCatalogJson(){
        Map<String,List<Catelog2Vo>> map = categoryService.getCatalogJson();
        return map;
    }
}
