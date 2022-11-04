package com.example.gulimall.seckill;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSeckillApplicationTests {

    @Test
    public void contextLoads() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println(now.plusDays(1));
        System.out.println(now.plusDays(2));
    }

}
