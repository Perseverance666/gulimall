# 谷粒商城 --- gulimall

### 项目介绍
谷粒商城项目是一套分布式电商项目，包括前台商城系统以及后台管理系统，基于 SpringCloud、SpringCloud Alibaba、MyBatis Plus等实现。
前台商城系统包括：用户登录、注册、商品搜索、商品详情、购物车、订单、秒杀活动等模块。
后台管理系统包括：系统管理、商品系统、优惠营销、库存系统、订单系统、用户系统、内容管理等七大模块

### 项目资源
1. 前端接口文档地址：https://easydoc.net/s/78237135/ZUqEdvA4/hKJTcbfd
2. 支付宝沙箱买家账号:rsguao0042@sandbox.com  支付密码:111111


### 使用说明
1. 新增商品、商品上架、全文检索、下单功能比较折磨人
2. 由于使用的用于内网穿透的natapp是免费隧道，重启更换域名，故需要修改nignx中gulimall.conf的监听域名
   以及application.yml中notify-url的域名
3. Sentinel启动：java -jar sentinel-dashboard-1.6.3.jar --server.port=8333(默认端口是8080，被后台系统占用了)
4. Sleuth整合Zipkin链路追踪需要去docker中启动容器，默认不是开机启动。里面的数据不是持久化的，
   若想持久化，可以存放到ElasticSearch中(生产环境再做这个)


### 组织结构

```
gulimall
├── gulimall-common -- 工具类及通用代码
├── renren-generator -- 人人开源项目的代码生成器
├── gulimall-auth-server -- 认证中心（社交登录、OAuth2.0、单点登录）
├── gulimall-cart -- 购物车服务
├── gulimall-coupon -- 优惠卷服务
├── gulimall-gateway -- 统一配置网关
├── gulimall-order -- 订单服务
├── gulimall-product -- 商品服务
├── gulimall-search -- 检索服务
├── gulimall-seckill -- 秒杀服务
├── gulimall-third-party -- 第三方服务
├── gulimall-ware -- 仓储服务
└── gulimall-member -- 会员服务

```

### 技术选型

**后端技术**

|        技术        |           说明           |                      官网                       |
| :----------------: | :----------------------: | :---------------------------------------------: |
|     SpringBoot     |       容器+MVC框架       |     https://spring.io/projects/spring-boot      |
|    SpringCloud     |        微服务架构        |     https://spring.io/projects/spring-cloud     |
| SpringCloudAlibaba |        一系列组件        | https://spring.io/projects/spring-cloud-alibaba |
|    MyBatis-Plus    |         ORM框架          |             https://mp.baomidou.com             |
|  renren-generator  | 人人开源项目的代码生成器 |   https://gitee.com/renrenio/renren-generator   |
|   Elasticsearch    |         搜索引擎         |    https://github.com/elastic/elasticsearch     |
|      RabbitMQ      |         消息队列         |            https://www.rabbitmq.com             |
|   SpringSession    |        分布式缓存        |    https://projects.spring.io/spring-session    |
|      Redisson      |         分布式锁         |      https://github.com/redisson/redisson       |
|       Docker       |       应用容器引擎       |             https://www.docker.com              |
|        OSS         |        对象云存储        |  https://github.com/aliyun/aliyun-oss-java-sdk  |

**前端技术**

|   技术    |    说明    |           官网            |
| :-------: | :--------: | :-----------------------: |
|    Vue    |  前端框架  |     https://vuejs.org     |
|  Element  | 前端UI框架 | https://element.eleme.io  |
| thymeleaf |  模板引擎  | https://www.thymeleaf.org |
|  node.js  | 服务端的js |   https://nodejs.org/en   |


### 环境搭建

#### 开发工具

|     工具      |        说明         |                      官网                       |
| :-----------: | :-----------------: | :---------------------------------------------: |
|     IDEA      |    开发Java程序     |     https://www.jetbrains.com/idea/download     |
| RedisDesktop  | redis客户端连接工具 |        https://redisdesktop.com/download        |
|  SwitchHosts  |    本地host管理     |       https://oldj.github.io/SwitchHosts        |
|    X-shell    |  Linux远程连接工具  | http://www.netsarang.com/download/software.html |
|    Navicat    |   数据库连接工具    |       http://www.formysql.com/xiazai.html       |
| PowerDesigner |   数据库设计工具    |             http://powerdesigner.de             |
|    Postman    |   API接口调试工具   |             https://www.postman.com             |
|    Jmeter     |    性能压测工具     |            https://jmeter.apache.org            |
|    Typora     |   Markdown编辑器    |                https://typora.io                |

#### 开发环境

|     工具      | 版本号 |                             下载                             |
| :-----------: | :----: | :----------------------------------------------------------: |
|      JDK      |  1.8   | https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html |
|     Mysql     |  5.7   |                    https://www.mysql.com                     |
|     Redis     | Redis  |                  https://redis.io/download                   |
| Elasticsearch | 7.6.2  |               https://www.elastic.co/downloads               |
|    Kibana     | 7.6.2  |               https://www.elastic.co/cn/kibana               |
|   RabbitMQ    | 3.8.5  |            http://www.rabbitmq.com/download.html             |
|     Nginx     | 1.1.6  |              http://nginx.org/en/download.html               |

注意：以上的除了jdk都是采用docker方式进行安装，详细安装步骤可参考百度!!!

#### 搭建步骤

> Windows环境部署

- 修改本机的host文件，映射域名端口

```
192.168.138.102	 gulimall.com
192.168.138.102	 search.gulimall.com
192.168.138.102  item.gulimall.com
192.168.138.102  auth.gulimall.com
192.168.138.102  cart.gulimall.com
192.168.138.102  order.gulimall.com
192.168.138.102  member.gulimall.com
192.168.138.102  seckill.gulimall.com
以上端口换成自己Linux的ip地址
```

- 修改Linux中Nginx的配置文件

```
1、在nginx.conf中添加负载均衡的配置    
user  nginx;
worker_processes  1;

error_log  /var/log/nginx/error.log warn;
pid        /var/run/nginx.pid;

events {
    worker_connections  1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;

    sendfile        on;

    keepalive_timeout  65;

    upstream gulimall{
        server 192.168.142.1:88;
    }

    include /etc/nginx/conf.d/*.conf;
}

2、在gulimall.conf中添加如下配置
server {
    listen       80;
    server_name  gulimall.com *.gulimall.com 3nvutx.natappfree.cc;

    location /static/ {
        root    /usr/share/nginx/html;
    }

    #解决内网穿透，请求Host头不匹配
    location /payed/ {
        #nginx代理给网关时，会丢失请求的host信息
        #只要是/payed/请求，转给网关，进入order服务
        proxy_set_header     Host order.gulimall.com;
        proxy_pass    http://gulimall;
    }

    location / {
        #nginx代理给网关时，会丢失请求的host信息
        proxy_set_header     Host $host;
        proxy_pass    http://gulimall;
    }
    
    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
}


```

- 克隆前端项目 `renren-fast-vue` 以 `npm run dev` 方式去运行
- 克隆整个后端项目 `gulimall` ，并导入 IDEA 中完成编译




