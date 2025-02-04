package com.lian.lianojbackendquestionservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.lian.lianojbackendquestionservice.mapper")
@EnableScheduling
@ComponentScan("com.lian")
public class LianojBackendQuestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LianojBackendQuestionServiceApplication.class, args);
    }

}
