package com.lian.lianojbackendquestionservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.lian.lianojbackendquestionservice.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.lian")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.lian.lianojbackendserviceclient.service"})
public class LianojBackendQuestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LianojBackendQuestionServiceApplication.class, args);
    }

}
