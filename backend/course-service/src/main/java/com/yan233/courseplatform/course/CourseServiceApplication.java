package com.yan233.courseplatform.course;

import com.yan233.courseplatform.common.runtime.LocalRuntimeState;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableCaching
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.yan233.courseplatform.course.mapper")
@SpringBootApplication(scanBasePackages = "com.yan233.courseplatform")
public class CourseServiceApplication {
    public static void main(String[] args) {
        LocalRuntimeState.configure("course-service");
        SpringApplication.run(CourseServiceApplication.class, args);
    }
}
