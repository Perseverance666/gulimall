package com.example.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.common.to.es.SkuEsModel;
import com.example.common.utils.R;
import com.example.gulimall.search.config.ElasticSearchConfig;
import com.example.gulimall.search.constant.EsConstant;
import com.example.gulimall.search.feign.ProductFeignService;
import com.example.gulimall.search.service.SearchService;
import com.example.gulimall.search.vo.AttrResponseVo;
import com.example.gulimall.search.vo.BrandVo;
import com.example.gulimall.search.vo.SearchParam;
import com.example.gulimall.search.vo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Date: 2022/10/10 17:48
 */

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private ProductFeignService productFeignService;

    /**
     * ????????????????????????SearchParam??????es???????????????????????????????????????SearchResult??????
     * @param param
     * @return
     */
    @Override
    public SearchResult search(SearchParam param) {
        SearchResult result = null;
        //1?????????????????????
        SearchRequest searchRequest = buildSearchRequest(param);
        try {
            //2?????????????????????
            SearchResponse response = client.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
            //3??????????????????????????????SearchResult
            result = buildSearchResult(response,param);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * ??????????????????
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * @param param
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        //??????????????????????????????DSL??????
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        //1?????????query-bool?????????????????????????????????????????????????????????????????????????????????????????????
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1???1??? must-match-????????????
        if(StringUtils.isNotEmpty(param.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",param.getKeyword()));
        }

        //1???2???filter-term-??????
        if(param.getCatalog3Id() != null){
            boolQuery.filter(QueryBuilders.termQuery("catalogId",param.getCatalog3Id()));
        }

        //1???3???filter-terms-??????
        if(param.getBrandId() != null && param.getBrandId().size() > 0){
            boolQuery.filter(QueryBuilders.termsQuery("brandId",param.getBrandId()));
        }

        //1???4???filter-nested-??????
        if(param.getAttrs() != null && param.getAttrs().size() > 0){
            for (String attrStr : param.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                //attrs=1_5???:8???&attrs=2_16G:8G
                String[] s = attrStr.split("_");
                String attrId = s[0];
                String[] attrValue = s[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue",attrValue));
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                //?????????attrs????????????????????????nested??????
                boolQuery.filter(nestedQuery);
            }
        }

        //1???5???filter-term-??????
        if(param.getHasStock() != null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock",param.getHasStock() == 1));
        }

        //1???6???filter-range-????????????
        if(StringUtils.isNotEmpty(param.getSkuPrice())){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            // param??????skuPrice?????????1_500/_500/500_
            String[] s = param.getSkuPrice().split("_");      //???_??????skuPrice
            if(s.length == 2){
                //1_500   1<= x <= 500
                rangeQuery.gte(s[0]).lte(s[1]);
            }else if(s.length == 1){
                if(param.getSkuPrice().startsWith("_")){
                    //_500     x <= 500
                    rangeQuery.lte(s[0]);
                }
                if(param.getSkuPrice().endsWith("_")){
                    //500_     x >= 500
                    rangeQuery.gte(s[0]);
                }
            }

            boolQuery.filter(rangeQuery);
        }

        //??????????????????
        sourceBuilder.query(boolQuery);


        //2?????????sort???from???size???highlight??? ????????????????????????
        //2???1???sort
        if(StringUtils.isNotEmpty(param.getSort())){
            //sort=saleCount_asc/desc
            String[] s = param.getSort().split("_");
            SortOrder sortOrder = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0],sortOrder);
        }

        //2???2???from???         from=(pageNum-1)*pageSize
        int from = (param.getPageNum() - 1) * EsConstant.PRODUCT_PAGE_SIZE;
        sourceBuilder.from(from);

        //2???3???size
        sourceBuilder.size(EsConstant.PRODUCT_PAGE_SIZE);

        //2???4???highlight     keyword???????????????????????????
        if(StringUtils.isNotEmpty(param.getKeyword())){
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }


        //3?????????aggs??? ????????????
        //3???1???brand_agg
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(10);
        //3???1??????1 brand_name_agg
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        //3???1??????2 brand_img_agg
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);

        //3???2???catalog_agg
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(10);
        //3???2??????1 catalog_name_agg
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);

        //3???3???attr_agg
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //3???3??????1 attr_id_agg
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(10);
        //3???3??????1???1??? attr_name_agg
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //3???3??????1???1??? attr_value_agg
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(10));
        attr_agg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(attr_agg);

        System.out.println("DSL:"+sourceBuilder.toString());

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX},sourceBuilder);
        return searchRequest;
    }

    /**
     * ??????????????????
     * @param response
     * @param param ???es??????????????????????????????SearchResult
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();

        //1??????????????????????????????????????????
        List<SkuEsModel> skuEsModels = new ArrayList<>();
        SearchHit[] hits = response.getHits().getHits();
        if(hits != null && hits.length > 0){
            for (SearchHit hit : hits) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);

                //??????skuTitle??????
                if(StringUtils.isNotEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highlightSkuTile = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(highlightSkuTile);
                }

                skuEsModels.add(skuEsModel);
            }
        }
        result.setProducts(skuEsModels);

        //2???1????????????-????????????
        result.setPageNum(param.getPageNum());
        //2???2????????????-????????????
        long total = response.getHits().getTotalHits().value;
        result.setTotal(total);
        //2???3????????????-?????????   
        int totalPages = (int)total % EsConstant.PRODUCT_PAGE_SIZE == 0 ?
                (int)total / EsConstant.PRODUCT_PAGE_SIZE : ((int)total / EsConstant.PRODUCT_PAGE_SIZE + 1);
        result.setTotalPages(totalPages);
        //2???4????????????-????????????
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        //------------------------??????????????????????????????????????????--------------------------

        //3???????????????????????????????????????????????????
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);

            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);

            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        //4???????????????????????????????????????????????????
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        for (Terms.Bucket bucket : catalog_agg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            long catalogId = bucket.getKeyAsNumber().longValue();
            catalogVo.setCatalogId(catalogId);

            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);

            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        //5???????????????????????????????????????????????????
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);

            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);

            ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValues = new ArrayList<>();
            for (Terms.Bucket attrValueBucket : attr_value_agg.getBuckets()) {
                String attrValue = attrValueBucket.getKeyAsString();
                attrValues.add(attrValue);
            }
            attrVo.setAttrValue(attrValues);


            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos);

        //6.???????????????????????????
        //6.1?????????
        if(param.getAttrs() != null && param.getAttrs().size() > 0){
            List<SearchResult.NavVo> navs = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //attrs=2_5???:6???
                String[] s = attr.split("_");
                //6.1.1????????????????????????????????????navName
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));

                //??????????????????????????????????????????????????????
                result.getAttrIds().add(Long.parseLong(s[0]));

                if(r.getCode() == 0){
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName());
                }else {
                    //??????????????????attrId?????????navName
                    navVo.setNavName(s[0]);
                }
                //6.1.2????????????????????????????????????navValue
                navVo.setNavValue(s[1]);
                //6.1.3????????????????????????????????????link    attrs= 15_?????????Hisilicon???
                String replace = replaceQueryString(param, "attrs",attr);
                navVo.setLink("http://search.gulimall.com/list.html?"+replace);

                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(navs);
        }

        //6.2??????
        if(param.getBrandId() != null && param.getBrandId().size() > 0){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("??????");
            //??????????????????????????????
            R r = productFeignService.brandInfos(param.getBrandId());
            if(r.getCode() == 0){
                List<BrandVo> brands = r.getData("brands", new TypeReference<List<BrandVo>>() {
                });
                StringBuffer stringBuffer = new StringBuffer();
                String replace = "";
                for (BrandVo brand : brands) {
                    stringBuffer.append(brand.getName()+";");
                    replace = replaceQueryString(param, "brandId", brand.getBrandId() + "");
                }
                navVo.setNavValue(stringBuffer.toString());
                navVo.setLink("http://search.gulimall.com/list.html?"+replace);
            }

            navs.add(navVo);
        }

        //TODO ??????????????????????????????


        return result;
    }

    /**
     * ?????????????????????key=value??????
     * @param param
     * @param value
     * @param key
     * @return
     */
    private String replaceQueryString(SearchParam param,String key, String value) {
        //??????????????????
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            //???????????????????????????java?????????
            encode = encode.replace("+","%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String replace = param.get_queryString().replace("&"+key+"=" + encode, "");
        return replace;
    }
}
