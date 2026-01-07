package com.mediarenamer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 媒体重命名系统主启动类
 */
@EnableCaching
@SpringBootApplication
public class MediaRenamerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaRenamerApplication.class, args);
    }

}
