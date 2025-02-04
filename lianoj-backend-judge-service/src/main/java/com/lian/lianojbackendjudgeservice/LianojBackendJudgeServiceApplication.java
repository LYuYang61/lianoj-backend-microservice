package com.lian.lianojbackendjudgeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.lian")
public class LianojBackendJudgeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LianojBackendJudgeServiceApplication.class, args);
    }

}
