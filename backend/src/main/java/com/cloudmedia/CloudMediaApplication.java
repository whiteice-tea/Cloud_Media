package com.cloudmedia;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@MapperScan("com.cloudmedia.mapper")
@ConfigurationPropertiesScan(basePackages = "com.cloudmedia.config")
public class CloudMediaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudMediaApplication.class, args);
    }
}
