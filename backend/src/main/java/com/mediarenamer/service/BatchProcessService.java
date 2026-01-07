package com.mediarenamer.service;

import com.mediarenamer.model.dto.TmdbMovieDTO;
import com.mediarenamer.model.dto.TmdbTvShowDTO;
import com.mediarenamer.model.dto.VideoFileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 批量处理服务
 * 使用虚拟线程进行高性能并发处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProcessService {

    private final TmdbService tmdbService;

    // 目录名清洗正则表达式
    private static final Pattern YEAR_PATTERN = Pattern.compile("[.\\s_-](\\d{4})[.\\s_-]");
    private static final Pattern RESOLUTION_PATTERN = Pattern.compile("[.\\s_-](?:720p|1080p|2160p|4K)[.\\s_-]", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELEASE_GROUP_PATTERN = Pattern.compile("[.\\s_-](?:CMCT|WiKi|CMCC|HDTV|BluRay|WEB-DL|WEBRip)[.\\s_-]", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRUCTURE_WORD_PATTERN = Pattern.compile("(?:Season\\s*\\d+|Disc\\s*\\d+|第.+碟|第.+季)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("[.\\s_-](?:法语|英语|中文|日语|韩语)[.\\s_-]");
    private static final Pattern EXTRA_SEPARATORS_PATTERN = Pattern.compile("[.\\s_-]+");


    /**
     * 批量匹配视频文件的媒体信息
     * 使用虚拟线程并发处理，大幅提升性能
     * 优化策略 V2（智能去重 + 批量预热）：
     * 1. 提取所有唯一的搜索关键词（去重）
     * 2. 批量并发预热缓存（避免重复 API 调用）
     * 3. 从缓存直接读取结果分配给文件
     * 4. 文件名、目录匹配异步并行（不再串行等待）
     *
     * @param videoFiles 待匹配的视频文件列表
     * @return 匹配成功的文件列表
     */
    public List<VideoFileDTO> batchMatchMedia(List<VideoFileDTO> videoFiles) {
        log.info("开始批量匹配媒体信息，总数: {}", videoFiles.size());
        long startTime = System.currentTimeMillis();

        // 过滤出未匹配的文件
        List<VideoFileDTO> unmatchedFiles = videoFiles.stream()
                .filter(file -> file.getMediaType() != null && !file.getMediaType().equals("UNKNOWN"))
                .toList();

        if (unmatchedFiles.isEmpty()) {
            log.info("没有需要匹配的文件");
            return videoFiles;
        }

        // 按类型分类处理
        List<VideoFileDTO> movies = new ArrayList<>();
        List<VideoFileDTO> tvShows = new ArrayList<>();

        for (VideoFileDTO file : unmatchedFiles) {
            if ("MOVIE".equals(file.getMediaType())) {
                movies.add(file);
            } else if ("TV_SHOW".equals(file.getMediaType())) {
                tvShows.add(file);
            }
        }

        log.info("待匹配: 电影={}, 电视剧={}", movies.size(), tvShows.size());

        // 使用虚拟线程执行器 (JDK 21+)
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // 1. 处理电影（智能去重 + 批量预热）
            if (!movies.isEmpty()) {
                CompletableFuture<Void> movieFuture = CompletableFuture.runAsync(() -> {
                    batchMatchMoviesOptimized(movies, executor);
                }, executor);
                futures.add(movieFuture);
            }

            // 2. 处理电视剧（智能去重 + 批量预热）
            if (!tvShows.isEmpty()) {
                CompletableFuture<Void> tvShowFuture = CompletableFuture.runAsync(() -> {
                    batchMatchTvShowsOptimized(tvShows, executor);
                }, executor);
                futures.add(tvShowFuture);
            }

            // 等待所有任务完成
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            // 设置超时时间 (5分钟)
            try {
                allOf.get(5, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                log.error("批量匹配超时");
                throw new RuntimeException("批量匹配超时", e);
            } catch (InterruptedException | ExecutionException e) {
                log.error("批量匹配失败", e);
                throw new RuntimeException("批量匹配失败", e);
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        long matchedCount = videoFiles.stream()
                .filter(f -> f.getMatchedInfo() != null)
                .count();

        log.info("批量匹配完成: 总数={}, 成功={}, 耗时={}ms, 匹配率={}/{} ({}%), 性能提升={}%",
                videoFiles.size(), matchedCount, elapsedTime,
                matchedCount, videoFiles.size(),
                String.format("%.1f", matchedCount * 100.0 / videoFiles.size()),
                String.format("%.1f", (2200.0 - elapsedTime) / 2200.0 * 100));

        return videoFiles;
    }

    /**
     * 批量匹配电影（优化版 - 智能去重 + 批量预热）
     * 核心优化：
     * 1. 提取所有唯一的搜索关键词（title + year）
     * 2. 批量并发预热缓存
     * 3. 从缓存读取结果分配给文件
     *
     * @param movies 电影列表
     * @param executor 虚拟线程执行器
     */
    private void batchMatchMoviesOptimized(List<VideoFileDTO> movies, ExecutorService executor) {
        log.info("开始批量匹配电影（优化版），总数: {}", movies.size());

        // 步骤 1: 去重收集唯一的搜索关键词
        record SearchKey(String title, Integer year) {}
        Map<SearchKey, List<VideoFileDTO>> keyToFilesMap = new ConcurrentHashMap<>();

        for (VideoFileDTO movie : movies) {
            SearchKey key = new SearchKey(movie.getParsedTitle(), movie.getParsedYear());
            keyToFilesMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(movie);
        }

        log.info("电影去重: 原始数量={}, 唯一搜索词={}, 去重率={}%",
                movies.size(), keyToFilesMap.size(),
                String.format("%.1f", (1.0 - keyToFilesMap.size() * 1.0 / movies.size()) * 100));

        // 步骤 2: 批量并发预热缓存
        List<CompletableFuture<Void>> preloadFutures = keyToFilesMap.keySet().stream()
                .map(key -> CompletableFuture.runAsync(() -> {
                    try {
                        // 调用 TmdbService，触发缓存写入
                        tmdbService.searchMovie(key.title(), key.year());
                    } catch (Exception e) {
                        log.debug("预热失败: title={}, year={}", key.title(), key.year());
                    }
                }, executor))
                .toList();

        // 等待所有预热完成
        try {
            CompletableFuture.allOf(preloadFutures.toArray(new CompletableFuture[0]))
                    .get(2, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("电影缓存预热失败", e);
        }

        // 步骤 3: 从缓存读取结果并分配
        for (var entry : keyToFilesMap.entrySet()) {
            SearchKey key = entry.getKey();
            List<VideoFileDTO> filesWithSameKey = entry.getValue();

            try {
                // 从缓存读取（速度极快）
                List<TmdbMovieDTO> results = tmdbService.searchMovie(key.title(), key.year());

                if (results != null && !results.isEmpty()) {
                    TmdbMovieDTO matchedInfo = results.getFirst();
                    // 将结果应用到所有具有相同搜索关键词的文件
                    for (VideoFileDTO file : filesWithSameKey) {
                        file.setMatchedInfo(matchedInfo);
                    }
                    log.debug("电影匹配成功: {} -> {}, 应用到 {} 个文件",
                            key.title(), matchedInfo.getTitle(), filesWithSameKey.size());
                }
            } catch (Exception e) {
                log.debug("电影匹配失败: title={}, year={}", key.title(), key.year());
            }
        }

        long matchedCount = movies.stream().filter(f -> f.getMatchedInfo() != null).count();
        log.info("电影匹配完成: 总数={}, 成功={}, 匹配率={}%",
                movies.size(), matchedCount,
                String.format("%.1f", matchedCount * 100.0 / movies.size()));
    }

    /**
     * 批量匹配电视剧（优化版 V3 - 智能级联匹配）
     * 核心优化：
     * 1. 文件名匹配优先（批量预热缓存）
     * 2. 只对未匹配文件进行目录匹配（避免重复）
     * 3. 目录匹配也进行去重
     * 4. 移除不必要的并行等待
     *
     * @param tvShows 电视剧列表
     * @param executor 虚拟线程执行器
     */
    private void batchMatchTvShowsOptimized(List<VideoFileDTO> tvShows, ExecutorService executor) {
        log.info("开始批量匹配电视剧（优化版），总数: {}", tvShows.size());
        long startTime = System.currentTimeMillis();

        // ===== 阶段 1: 文件名匹配（批量预热缓存）=====
        record SearchKey(String title, Integer year) {}
        Map<SearchKey, List<VideoFileDTO>> fileNameKeyMap = new ConcurrentHashMap<>();

        for (VideoFileDTO tvShow : tvShows) {
            SearchKey key = new SearchKey(tvShow.getParsedTitle(), tvShow.getParsedYear());
            fileNameKeyMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(tvShow);
        }

        log.info("文件名去重: 原始数量={}, 唯一搜索词={}, 去重率={}%",
                tvShows.size(), fileNameKeyMap.size(),
                String.format("%.1f", (1.0 - fileNameKeyMap.size() * 1.0 / tvShows.size()) * 100));

        // 批量预热缓存
        List<CompletableFuture<Void>> preloadFutures = fileNameKeyMap.keySet().stream()
                .map(key -> CompletableFuture.runAsync(() -> {
                    try {
                        tmdbService.searchTvShow(key.title(), key.year());
                    } catch (Exception e) {
                        log.debug("预热失败: title={}, year={}", key.title(), key.year());
                    }
                }, executor))
                .toList();

        try {
            CompletableFuture.allOf(preloadFutures.toArray(new CompletableFuture[0]))
                    .get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("文件名缓存预热失败", e);
        }

        // 从缓存读取并分配结果
        for (var entry : fileNameKeyMap.entrySet()) {
            SearchKey key = entry.getKey();
            List<VideoFileDTO> filesWithSameKey = entry.getValue();

            try {
                List<TmdbTvShowDTO> results = tmdbService.searchTvShow(key.title(), key.year());
                if (results != null && !results.isEmpty()) {
                    TmdbTvShowDTO matchedInfo = results.getFirst();
                    for (VideoFileDTO file : filesWithSameKey) {
                        file.setMatchedInfo(matchedInfo);
                    }
                    log.debug("文件名匹配成功: {} -> {}, 应用到 {} 个文件",
                            key.title(), matchedInfo.getName(), filesWithSameKey.size());
                }
            } catch (Exception e) {
                log.debug("文件名匹配失败: {}", key.title());
            }
        }

        long fileNameMatchedCount = tvShows.stream().filter(f -> f.getMatchedInfo() != null).count();
        log.info("文件名匹配完成: 成功={}/{}, 耗时={}ms",
                fileNameMatchedCount, tvShows.size(),
                System.currentTimeMillis() - startTime);

        // 如果全部匹配成功，直接返回
        if (fileNameMatchedCount == tvShows.size()) {
            log.info("电视剧匹配完成: 总数={}, 成功={}, 匹配率=100.0%", tvShows.size(), fileNameMatchedCount);
            return;
        }

        // ===== 阶段 2: 目录匹配（仅处理未匹配的文件）=====
        List<VideoFileDTO> unmatchedFiles = tvShows.stream()
                .filter(f -> f.getMatchedInfo() == null)
                .toList();

        log.info("剩余未匹配文件: {}, 开始目录匹配", unmatchedFiles.size());

        // 祖父目录分组（去重）
        Map<String, List<VideoFileDTO>> groupedByGrandParent = unmatchedFiles.stream()
                .filter(f -> f.getGrandParentDirectory() != null && !f.getGrandParentDirectory().isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(
                        VideoFileDTO::getGrandParentDirectory,
                        ConcurrentHashMap::new,
                        java.util.stream.Collectors.toCollection(CopyOnWriteArrayList::new)
                ));

        // 父目录分组（去重）
        Map<String, List<VideoFileDTO>> groupedByParent = unmatchedFiles.stream()
                .filter(f -> f.getParentDirectory() != null && !f.getParentDirectory().isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(
                        VideoFileDTO::getParentDirectory,
                        ConcurrentHashMap::new,
                        java.util.stream.Collectors.toCollection(CopyOnWriteArrayList::new)
                ));

        // 预热祖父目录缓存
        List<CompletableFuture<Void>> grandParentPreload = groupedByGrandParent.keySet().stream()
                .map(dir -> CompletableFuture.runAsync(() -> {
                    try {
                        String cleaned = cleanDirectoryName(dir);
                        tmdbService.searchTvShow(cleaned, null);
                    } catch (Exception e) {
                        log.debug("祖父目录预热失败: {}", dir);
                    }
                }, executor))
                .toList();

        // 预热父目录缓存
        List<CompletableFuture<Void>> parentPreload = groupedByParent.keySet().stream()
                .map(dir -> CompletableFuture.runAsync(() -> {
                    try {
                        String cleaned = cleanDirectoryName(dir);
                        tmdbService.searchTvShow(cleaned, null);
                    } catch (Exception e) {
                        log.debug("父目录预热失败: {}", dir);
                    }
                }, executor))
                .toList();

        // 等待目录缓存预热完成
        try {
            CompletableFuture.allOf(
                    Stream.concat(grandParentPreload.stream(), parentPreload.stream())
                            .toArray(CompletableFuture[]::new)
            ).get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("目录缓存预热失败", e);
        }

        // 从缓存读取并分配祖父目录结果
        for (var entry : groupedByGrandParent.entrySet()) {
            String grandParentDir = entry.getKey();
            List<VideoFileDTO> files = entry.getValue();

            try {
                String cleaned = cleanDirectoryName(grandParentDir);
                List<TmdbTvShowDTO> results = tmdbService.searchTvShow(cleaned, null);

                if (results != null && !results.isEmpty()) {
                    TmdbTvShowDTO matchedInfo = results.getFirst();
                    for (VideoFileDTO file : files) {
                        if (file.getMatchedInfo() == null) {
                            file.setMatchedInfo(matchedInfo);
                        }
                    }
                    log.debug("祖父目录匹配: {} -> {}, 应用到 {} 个文件",
                            grandParentDir, matchedInfo.getName(), files.size());
                }
            } catch (Exception e) {
                log.debug("祖父目录匹配失败: {}", grandParentDir);
            }
        }

        // 从缓存读取并分配父目录结果
        for (var entry : groupedByParent.entrySet()) {
            String parentDir = entry.getKey();
            List<VideoFileDTO> files = entry.getValue();

            try {
                String cleaned = cleanDirectoryName(parentDir);
                List<TmdbTvShowDTO> results = tmdbService.searchTvShow(cleaned, null);

                if (results != null && !results.isEmpty()) {
                    TmdbTvShowDTO matchedInfo = results.getFirst();
                    for (VideoFileDTO file : files) {
                        if (file.getMatchedInfo() == null) {
                            file.setMatchedInfo(matchedInfo);
                        }
                    }
                    log.debug("父目录匹配: {} -> {}, 应用到 {} 个文件",
                            parentDir, matchedInfo.getName(), files.size());
                }
            } catch (Exception e) {
                log.debug("父目录匹配失败: {}", parentDir);
            }
        }

        long matchedCount = tvShows.stream().filter(f -> f.getMatchedInfo() != null).count();
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("电视剧匹配完成: 总数={}, 成功={}, 匹配率={}%, 耗时={}ms",
                tvShows.size(), matchedCount,
                String.format("%.1f", matchedCount * 100.0 / tvShows.size()),
                totalTime);
    }

    /**
     * 清洗目录名,移除干扰信息
     * 处理规则:
     * - 移除年份: 2006
     * - 移除分辨率: 1080p, 720p
     * - 移除发布组信息: CMCT, WiKi
     * - 移除结构词: Season 1, Disc 1, 第一碟
     * - 移除语言标识: 法语, 英语
     *
     * @param directoryName 原始目录名
     * @return 清洗后的目录名
     */
    private String cleanDirectoryName(String directoryName) {
        if (directoryName == null || directoryName.trim().isEmpty()) {
            return directoryName;
        }

        String cleaned = directoryName;

        // 移除年份 (四位数字)
        cleaned = YEAR_PATTERN.matcher(cleaned).replaceAll(" ");

        // 移除分辨率
        cleaned = RESOLUTION_PATTERN.matcher(cleaned).replaceAll(" ");

        // 移除发布组信息
        cleaned = RELEASE_GROUP_PATTERN.matcher(cleaned).replaceAll(" ");

        // 移除结构词 (Season, Disc, 第xx碟)
        cleaned = STRUCTURE_WORD_PATTERN.matcher(cleaned).replaceAll(" ");

        // 移除语言标识
        cleaned = LANGUAGE_PATTERN.matcher(cleaned).replaceAll(" ");

        // 清理多余的分隔符,统一为单个空格
        cleaned = EXTRA_SEPARATORS_PATTERN.matcher(cleaned).replaceAll(" ").trim();

        // 如果清洗后为空,返回原目录名
        if (cleaned.isEmpty()) {
            log.debug("目录名清洗后为空,返回原目录名: {}", directoryName);
            return directoryName;
        }

        // 记录清洗结果
        if (!cleaned.equals(directoryName)) {
            log.debug("目录名清洗: {} -> {}", directoryName, cleaned);
        }

        return cleaned;
    }
}
