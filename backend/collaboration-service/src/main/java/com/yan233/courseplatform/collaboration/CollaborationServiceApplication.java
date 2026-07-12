package com.yan233.courseplatform.collaboration;

import com.yan233.courseplatform.common.runtime.LocalRuntimeState;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.yan233.courseplatform.collaboration.mapper")
@SpringBootApplication(scanBasePackages = "com.yan233.courseplatform")
public class CollaborationServiceApplication {
    public static void main(String[] args) {
        LocalRuntimeState.configure("collaboration-service");
        SpringApplication.run(CollaborationServiceApplication.class, args);
    }
}
