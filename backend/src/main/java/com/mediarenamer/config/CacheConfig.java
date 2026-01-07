package com.mediarenamer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类 (优化版)
 * 使用 Caffeine 作为本地缓存实现,优化 TMDB API 调用性能
 *
 * 优化策略:
 * - 为不同类型的缓存设置不同的过期时间和容量
 * - 搜索结果缓存 1 小时 (数据可能变化)
 * - 详情和季度信息缓存 24 小时 (数据相对稳定)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置 Caffeine 缓存管理器
     * 使用不同的缓存策略优化性能
     *
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // 配置多个缓存实例，每个有不同的策略
        cacheManager.setCaches(Arrays.asList(
                // 电影搜索缓存: 1小时过期, 最大 2000 条
                buildCache("movieSearch", 1, TimeUnit.HOURS, 2000),

                // 电影详情缓存: 24小时过期, 最大 1000 条
                buildCache("movieDetails", 24, TimeUnit.HOURS, 1000),

                // 电视剧搜索缓存: 1小时过期, 最大 2000 条
                buildCache("tvSearch", 1, TimeUnit.HOURS, 2000),

                // 电视剧详情缓存: 24小时过期, 最大 1000 条
                buildCache("tvDetails", 24, TimeUnit.HOURS, 1000),

                // 季度集数缓存: 24小时过期, 最大 5000 条 (最常用)
                buildCache("seasonEpisodes", 24, TimeUnit.HOURS, 5000),

                // 季度剧集详情缓存（含标题）: 24小时过期, 最大 5000 条
                buildCache("seasonEpisodesDetail", 24, TimeUnit.HOURS, 5000)
        ));

        return cacheManager;
    }

    /**
     * 构建缓存实例
     *
     * @param name 缓存名称
     * @param duration 过期时长
     * @param timeUnit 时间单位
     * @param maxSize 最大容量
     * @return CaffeineCache
     */
    private CaffeineCache buildCache(String name, long duration, TimeUnit timeUnit, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(duration, timeUnit)
                .maximumSize(maxSize)
                .recordStats() // 启用统计
                .build());
    }
}
