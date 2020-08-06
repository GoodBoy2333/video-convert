package com.fy.cv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.fy.cv.mapper")
@EnableScheduling
public class VideoConvertWebApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(VideoConvertWebApplication.class);
    }
    public static void main(String[] args) {
        SpringApplication.run(VideoConvertWebApplication.class, args);
    }

}
