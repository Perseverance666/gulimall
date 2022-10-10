package com.example.gulimall.search.service.impl;

import com.example.gulimall.search.service.MallSearchService;
import com.example.gulimall.search.vo.SearchParam;
import com.example.gulimall.search.vo.SearchResult;
import org.springframework.stereotype.Service;

/**
 * @Date: 2022/10/10 17:48
 */

@Service
public class MallSearchServiceImpl implements MallSearchService {

    /**
     * 根据页面请求参数SearchParam，去es中查询数据，并将结果封装成SearchResult返回
     * @param param
     * @return
     */
    @Override
    public SearchResult search(SearchParam param) {
        //1、动态构建出查询需要的DSL语句

        return null;
    }
}
