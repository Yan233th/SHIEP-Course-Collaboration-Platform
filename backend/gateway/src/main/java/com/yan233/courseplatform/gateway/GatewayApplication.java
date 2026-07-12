package com.yan233.courseplatform.gateway;

import com.yan233.courseplatform.common.runtime.LocalRuntimeState;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.yan233.courseplatform")
public class GatewayApplication {
    public static void main(String[] args) {
        LocalRuntimeState.configure("gateway");
        SpringApplication.run(GatewayApplication.class, args);
    }
}
