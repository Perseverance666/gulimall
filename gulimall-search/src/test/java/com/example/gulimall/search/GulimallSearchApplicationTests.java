package com.example.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.example.gulimall.search.config.ElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @Data
    public static class Account {
        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }


    /**
     * 复杂检索:在bank中搜索address中包含mill的所有人的年龄分布以及平均年龄，平均薪资
     */
    @Test
    public void searchData() throws IOException {
        //1. 创建检索请求
        SearchRequest request = new SearchRequest();
        //1.1）指定索引
        request.indices("bank");
        //1.2）构造检索条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("address", "Mill"));

        //1.2.1)按照年龄分布进行聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("ageAgg").field("age").size(10));
        //1.2.2)计算平均年龄
        sourceBuilder.aggregation(AggregationBuilders.avg("ageAvg").field("age"));
        //1.2.3)计算平均薪资
        sourceBuilder.aggregation(AggregationBuilders.avg("balanceAvg").field("balance"));

        System.out.println("检索条件：" + sourceBuilder);
        request.source(sourceBuilder);

        //2. 执行检索
        SearchResponse searchResponse = client.search(request, ElasticSearchConfig.COMMON_OPTIONS);
        System.out.println("检索结果：" + searchResponse);

        //3. 将检索结果封装为Bean
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit searchHit : searchHits) {
            String source = searchHit.getSourceAsString();
            Account account = JSON.parseObject(source, Account.class);
            System.out.println(account);
        }

        //4. 获取聚合信息
        Aggregations aggregations = searchResponse.getAggregations();
        Terms ageTerms = aggregations.get("ageAgg");
        for (Terms.Bucket bucket :ageTerms.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄：" + keyAsString + " ==> " + bucket.getDocCount());
        }
        Avg ageAvg = aggregations.get("ageAvg");
        System.out.println("平均年龄：" + ageAvg.getValue());

        Avg balanceAvg = aggregations.get("balanceAvg");
        System.out.println("平均薪资：" + balanceAvg.getValue());

    }

    /**
     * 测试存储数据到es
     * 更新也可以
     */
    @Test
    public void indexData() throws IOException {
        //创建新增请求
        IndexRequest request = new IndexRequest("users");
        request.id("1"); //数据的id
        User user = new User();
        user.setUserName("zhangsan");
        user.setGender("男");
        user.setAge(18);
        String jsonString = JSON.toJSONString(user);
        request.source(jsonString, XContentType.JSON); //要保存的内容

        //执行新增
        IndexResponse indexResponse = client.index(request, ElasticSearchConfig.COMMON_OPTIONS);

        //提取有用的响应数据
        System.out.println(indexResponse);

    }

    @Data
    public class User {
        private String userName;
        private String gender;
        private Integer age;
    }

}
