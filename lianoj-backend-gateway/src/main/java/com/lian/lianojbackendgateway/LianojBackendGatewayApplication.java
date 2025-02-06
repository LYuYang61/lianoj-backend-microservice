package com.lian.lianojbackendgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LianojBackendGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(LianojBackendGatewayApplication.class, args);
    }

}
