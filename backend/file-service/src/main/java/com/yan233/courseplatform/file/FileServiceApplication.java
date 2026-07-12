package com.yan233.courseplatform.file;

import com.yan233.courseplatform.common.runtime.LocalRuntimeState;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.yan233.courseplatform.file.mapper")
@SpringBootApplication(scanBasePackages = "com.yan233.courseplatform")
public class FileServiceApplication {
    public static void main(String[] args) {
        LocalRuntimeState.configure("file-service");
        SpringApplication.run(FileServiceApplication.class, args);
    }
}
