package com.example.gulimall.search.service;

import com.example.common.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

/**
 * @Date: 2022/10/4 17:34
 */


public interface ProductSaveService {
    boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
