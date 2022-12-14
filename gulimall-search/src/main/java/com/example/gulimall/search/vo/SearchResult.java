package com.example.gulimall.search.vo;
import com.example.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Date: 2022/10/10 18:10
 *
 * 根据页面中的请求参数SearchParam去es中查询 返回给页面的结果vo
 */
@Data
public class SearchResult {

    private List<SkuEsModel> products;  //查询到的所有商品信息

    /**
     * 以下是分页信息
     */
    private Integer pageNum;            //当前页码
    private Long total;                 //总记录数
    private Integer totalPages;         //总页码
    private List<Integer> pageNavs;     //导航页码

    private List<BrandVo> brands;       //当前查询到的结果，所有涉及到的品牌
    private List<CatalogVo> catalogs;   //当前查询到的结果，所有涉及到的所有分类
    private List<AttrVo> attrs;         //当前查询到的结果，所有涉及到的所有属性

    //==========以上是返回给页面的所有信息============


    //面包屑导航数据
    private List<NavVo> navs = new ArrayList<>();
    //哪些属性已经被用来筛选了，前端可不显示了
    private List<Long> attrIds = new ArrayList<>();

    @Data
    public static class NavVo{
        private String navName;         //面包屑数据名字
        private String navValue;        //面包屑数据值
        private String link;            //取消面包屑后，跳转的地址
    }

    @Data
    public static class BrandVo{
        private Long brandId;
        private String brandName;

        private String brandImg;
    }

    @Data
    public static class CatalogVo{
        private Long catalogId;
        private String catalogName;

    }

    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;

        private List<String> attrValue;
    }
}