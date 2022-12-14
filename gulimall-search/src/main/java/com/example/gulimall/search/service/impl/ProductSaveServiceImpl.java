package com.example.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.common.to.es.SkuEsModel;
import com.example.gulimall.search.config.ElasticSearchConfig;
import com.example.gulimall.search.constant.EsConstant;
import com.example.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Date: 2022/10/4 17:34
 */

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {
    @Autowired
    private RestHighLevelClient client;

    /**
     * 上架商品
     * 将数据存储到es中
     * @param skuEsModels
     * @return
     */
    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        //1、在es中建立product索引，建立好映射关系
        //2、将数据保存到es中
        BulkRequest bulkRequest = new BulkRequest();
        skuEsModels.forEach(skuEsModel -> {
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(skuEsModel.getSkuId().toString());
            String json = JSON.toJSONString(skuEsModel);
            indexRequest.source(json, XContentType.JSON);
            bulkRequest.add(indexRequest);
        });
        BulkResponse bulk = client.bulk(bulkRequest, ElasticSearchConfig.COMMON_OPTIONS);

        //TODO 如果批量错误
        boolean hasFailures = bulk.hasFailures();
        List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.info("商品上架完成：{}",collect);

        return hasFailures;

    }
}
