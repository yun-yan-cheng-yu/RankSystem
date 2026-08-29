package com.zqyyz.ranksystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.zqyyz.ranksystem")
@MapperScan("com.zqyyz.ranksystem.mapper")  // 扫描 mapper 包下的所有接口
public class RankSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RankSystemApplication.class, args);
    }
}
