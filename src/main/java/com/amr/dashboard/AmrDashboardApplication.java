package com.amr.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
@EnableDiscoveryClient
public class AmrDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmrDashboardApplication.class, args);
    }
}
