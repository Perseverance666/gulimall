package com.atguigu.gulimall.ssoserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * 单点登录流程：
 *  1、第一个客户端访问服务器，由于未登录（session中没有token对应的用户信息），并且请求参数没有token，跳转到sso-server服务器
 *  （可以理解为认证中心，只要客户端在自己的服务器上没登录，不管别的客户端是否登录，都会来到认证中心），并携带着自己的redirect_url，
 *    即登录后要去的地方
 *  2、来到认证中心后，输入用户信息进行登录，登录成功后，将token对应的用户信息存入session（存入redis），并在认证中心创建cookie，
 *     里面存放token，只要认证中心有这个cookie，就证明已经有一个客户端登录过了
 *  3、然后由认证中心跳转到客户端指定redirect_url的位置，请求参数中携带token，只要有token，就会去获取认证中心session存放的
 *     token对应的用户信息，并将token对应的用户信息存入自己服务器中的session，第一个客户端登录成功
 *  4、第二个客户端访问服务器，由于未登录（session中没有token对应的用户信息，说明在自己的服务器上没登录），跳转到sso-server服务器，
 *     并携带着自己的redirect_url
 *  5、来到认证中心，由于认证中心有存放token的cookie，证明已经有客户端登录过了，不用再登录，直接跳转指定redirect_url的位置，
 *     请求参数中携带token，只要有token，就会去获取认证中心session存放的token对应的用户信息，并将token对应的用户信息存入
 *     自己服务器中的session，第二个客户端登录成功
 *
 * 关键点：
 *  1.客户端只要自己没登录都会先进入认证中心
 *  2.有人登录，认证中心就会留下cookie存放token
 *  3.客户端请求参数有token，就会获取认证中心session存放的token对应用户信息
 *  4.客户端自己session中有token对应信息，即登录成功
 */

@SpringBootApplication
public class GulimallTestSsoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallTestSsoServerApplication.class, args);
    }

}
