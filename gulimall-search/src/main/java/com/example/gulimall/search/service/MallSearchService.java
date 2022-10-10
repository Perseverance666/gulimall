package com.example.gulimall.search.service;

import com.example.gulimall.search.vo.SearchParam;
import com.example.gulimall.search.vo.SearchResult;

/**
 * @Date: 2022/10/10 17:48
 */


public interface MallSearchService {
    SearchResult search(SearchParam param);
}
