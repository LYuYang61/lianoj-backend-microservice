package com.lian.lianojbackenduserservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.lian.lianojbackenduserservice.mapper")
@EnableScheduling
@ComponentScan("com.lian")
public class LianojBackendUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LianojBackendUserServiceApplication.class, args);
    }

}
