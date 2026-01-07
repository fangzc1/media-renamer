package com.mediarenamer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TMDB API 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "tmdb")
public class TmdbProperties {

    private String apiKey;
    private String apiUrl;
    private String language;
    private RateLimit rateLimit;
    private Timeout timeout;

    @Data
    public static class RateLimit {
        private int requests;
        private int period;
    }

    @Data
    public static class Timeout {
        private int connect = 10000;  // 默认 10 秒
        private int read = 30000;     // 默认 30 秒
    }

}
