package com.lian.lianojbackenduserservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.lian.lianojbackenduserservice.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.lian")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.lian.lianojbackendserviceclient.service"})
public class LianojBackendUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LianojBackendUserServiceApplication.class, args);
    }

}
