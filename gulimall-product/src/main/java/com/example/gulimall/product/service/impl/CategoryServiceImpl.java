package com.example.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gulimall.product.service.CategoryBrandRelationService;
import com.example.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.CategoryDao;
import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取所有分类及子分类
     * 并返回json树形结构
     * 商品系统，分类维护 以及 平台属性左侧的列表展示
     *
     * @return
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        //查询所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //查询一级分类，并设置其子分类
        List<CategoryEntity> level1Menus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren(getChildren(menu,entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }
    /**
     * 递归找出该分类下的所有子类
     *
     * @param root
     * @param all
     * @return
     */
    public List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter((category) -> {
            return category.getParentCid() == root.getCatId();
        }).map((menu) -> {
            menu.setChildren(getChildren(menu,all));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }

    /**
     * 商品系统，分类维护，批量删除
     * @param asList
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 检查当前删除的菜单，是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }

    /**
     * 找出指定catelogId的全路径
     * @param catelogId
     * @return
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        //结果是[225,25,2]，注意翻转
        Collections.reverse(parentPath);

        return parentPath.toArray(new Long[parentPath.size()]);
    }
    /**
     * 递归查询 返回指定catelogId的所有父节点catelogId
     * 返回结果例：[225,25,2]
     * @param catelogId
     * @param paths
     * @return
     */
    public List<Long> findParentPath(Long catelogId,List<Long> paths){
        //先将该catelogId添加到paths中
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid() != 0){
            //有父节点
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());

        //TODO 更新其他关联
    }

    /**
     * 查询所有一级分类
     * @return
     */
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        LambdaQueryWrapper<CategoryEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(CategoryEntity::getCatLevel,1);
        List<CategoryEntity> entities = this.baseMapper.selectList(lqw);
        return entities;
    }

    /**
     * TODO 产生堆外内存溢出：OutOfDirectMemoryError
     * 1）、springboot2.0以后默认使用lettuce作为操作redis的客户端。它使用netty进行网络通信。
     * 2）、lettuce的bug导致netty堆外内存溢出 -Xmx300m；netty如果没有指定堆外内存，默认使用-Xmx300m
     *     可以通过-Dio.netty.maxDirectMemory进行设置
     *     解决方案：不能使用-Dio.netty.maxDirectMemory只去调大堆外内存。
     *     1）、升级lettuce客户端。   2）、切换使用jedis
     *      redisTemplate：
     *      lettuce、jedis操作redis的底层客户端。Spring再次封装redisTemplate；
     *
     *
     * 从redis中查询并封装分类数据
     * 返回map类型数据，key为一级分类id，value为Catelog2Vo类型数据
     * @return
     */
    @Override
    public Map<String,List<Catelog2Vo>> getCatalogJson() {
        /**
         * 1、空结果缓存：解决缓存穿透
         * 2、设置过期时间（加随机值）：解决缓存雪崩
         * 3、加锁：解决缓存击穿
         */
        //先判断redis缓存中是否有数据
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String catalogJson = ops.get("catalogJson");
        if(StringUtils.isEmpty(catalogJson)){
            //缓存中没有数据，从数据库中查数据
            System.out.println("缓存不命中....将要查询数据库...");
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = this.getCatalogJsonFromDBWithRedisLock();
            return catalogJsonFromDB;
        }
        System.out.println("缓存命中....直接返回....");
        //缓存中有数据，将缓存中数据由String转为指定类型
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
        return result;
    }

    /**
     * 从缓存或者数据库中查询并封装分类数据
     * 返回map类型数据，key为一级分类id，value为Catelog2Vo类型数据
     * @return
     */
    public Map<String,List<Catelog2Vo>> getDataFromDB() {
        //先确认redis缓存中是否有数据
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String catalogJson = ops.get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            //缓存不为null直接返回
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }

        System.out.println("将要查询数据库.....");

        //TODO 优化：将数据库由原来的多次查询变为一次查询
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1、查询所有一级分类
        List<CategoryEntity> level1Categories = getCategoryEntitiesByParentCid(selectList,0L);
        Map<String, List<Catelog2Vo>> map = null;
        if(level1Categories != null){

            //2、封装成map，key为一级分类id，value为Catelog2Vo类型数据
            map = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //查询该一级分类下的二级分类
                List<CategoryEntity> level2Categories = getCategoryEntitiesByParentCid(selectList,v.getCatId());

                //将所有2级分类封装成Catelog2Vo
                List<Catelog2Vo> catelog2Vos = null;
                if (level2Categories != null) {
                    catelog2Vos = level2Categories.stream().map(l2 -> {
                        Catelog2Vo catelog2Vo = new Catelog2Vo();
                        catelog2Vo.setCatalog1Id(v.getCatId().toString());
                        catelog2Vo.setId(l2.getCatId().toString());
                        catelog2Vo.setName(l2.getName());

                        //查询该2级分类下的所有3级分类
                        List<CategoryEntity> level3Categories= getCategoryEntitiesByParentCid(selectList,l2.getCatId());
                        List<Catelog2Vo.Catelog3Vo> catelog3Vos = null;
                        if(level3Categories != null){
                            //封装Catelog3Vo类型数据
                            catelog3Vos = level3Categories.stream().map(l3 -> {
                                Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo();
                                catelog3Vo.setCatalog2Id(l2.getCatId().toString());
                                catelog3Vo.setId(l3.getCatId().toString());
                                catelog3Vo.setName(l3.getName());
                                return catelog3Vo;
                            }).collect(Collectors.toList());
                        }

                        catelog2Vo.setCatalog3List(catelog3Vos);
                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }

                return catelog2Vos;

            }));
        }

        //3、查到的数据再放入缓存，将对象转为json放在缓存中
        String jsonString = JSON.toJSONString(map);
        ops.set("catalogJson",jsonString,1, TimeUnit.DAYS);
        return map;
    }
    /**
     * 抽出的方法，根据parentId查询子类数据
     * @param selectList
     * @param parent_cid
     * @return
     */
    private List<CategoryEntity> getCategoryEntitiesByParentCid(List<CategoryEntity> selectList,Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid)
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * 加本地锁synchronized (this) 获取数据
     * TODO 本地锁：synchronized，JUC（Lock），在分布式情况下，想要锁住所有，必须使用分布式锁
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithLocalLock() {
        //只要是同一把锁，就能锁住需要这个锁的所有线程
        //1、synchronized (this)：SpringBoot所有的组件在容器中都是单例的。
        synchronized (this) {
            //得到锁以后，我们应该再去缓存中确定一次，如果没有才需要继续查询
            return getDataFromDB();
        }
    }

    /**
     * 加redis锁 setnx命令 获取数据
     * 加锁保证原子性(set NX EX)，删锁也要保证原子性(lua脚本解锁)
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedisLock() {
        //1、占分布式锁。去redis占坑，并设置过期时间(必须和加锁是同步的，原子性)
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid,300,TimeUnit.SECONDS);
        if(lock){
            System.out.println("获取分布式锁成功...");
            //加锁成功...执行业务
            Map<String, List<Catelog2Vo>> dataFromDB = null;
            try {
                 dataFromDB = getDataFromDB();
            }finally {          //无论业务是否成功，都要删锁
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                        "then return redis.call('del', KEYS[1]) else return 0 end";
                //删除锁(lua脚本解锁)
                stringRedisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Arrays.asList("lock"),uuid);
                return dataFromDB;
            }
        }else{
            //加锁失败...重试。synchronized ()
            System.out.println("获取分布式锁失败...等待重试");
            try {
                //休眠200ms重试
                Thread.sleep(200);
            } catch (Exception e) {

            }
            return getCatalogJsonFromDBWithRedisLock();  //自旋的方式
        }
    }



}