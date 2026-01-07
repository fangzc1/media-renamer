package com.mediarenamer.service;

import com.mediarenamer.model.dto.RenamePreviewDTO;
import com.mediarenamer.model.dto.TmdbMovieDTO;
import com.mediarenamer.model.dto.TmdbTvShowDTO;
import com.mediarenamer.model.dto.VideoFileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

/**
 * 重命名服务
 * 负责生成新文件名、预览重命名、执行批量重命名
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RenameService {

    // 注入 TmdbService 用于获取季度信息
    private final TmdbService tmdbService;

    // 注入 OrganizationService 用于整理未处理文件
    private final OrganizationService organizationService;

    // 目录创建锁，防止并发创建同一目录
    private final ConcurrentHashMap<String, Object> directoryLocks = new ConcurrentHashMap<>();

    /**
     * 电影命名模板枚举
     */
    public enum MovieTemplate {
        SIMPLE("{title} ({year}).{ext}"),
        STANDARD("{title} ({year})/{title} ({year}).{ext}"),
        DETAILED("{title} ({year})/{title} ({year}) - {resolution}.{ext}");

        private final String template;

        MovieTemplate(String template) {
            this.template = template;
        }

        public String getTemplate() {
            return template;
        }
    }

    /**
     * 电视剧命名模板枚举
     */
    public enum TvTemplate {
        STANDARD("{show} ({year})/Season {season:02d}/{show} - S{season:02d}E{episode:02d} - {title}.{ext}"),
        WITH_TITLE("{show}/Season {season:02d}/{show} - S{season:02d}E{episode:02d} - {title}.{ext}"),
        COMPACT("{show}/S{season:02d}E{episode:02d}.{ext}");

        private final String template;

        TvTemplate(String template) {
            this.template = template;
        }

        public String getTemplate() {
            return template;
        }
    }

    /**
     * 根据电影信息和模板生成新文件名
     *
     * @param videoFile 原视频文件信息
     * @param movieInfo TMDB 电影信息
     * @param template  命名模板
     * @return 重命名预览
     */
    public RenamePreviewDTO generateMovieRename(VideoFileDTO videoFile,
                                                TmdbMovieDTO movieInfo,
                                                MovieTemplate template) {
        try {
            String newFileName = applyMovieTemplate(template.getTemplate(), movieInfo, videoFile);
            return buildRenamePreview(videoFile, newFileName, movieInfo, null);
        } catch (Exception e) {
            log.error("生成电影重命名失败: {}", videoFile.getFilePath(), e);
            return buildErrorPreview(videoFile, e.getMessage());
        }
    }

    /**
     * 根据电视剧信息和模板生成新文件名
     *
     * @param videoFile 原视频文件信息
     * @param tvInfo    TMDB 电视剧信息
     * @param template  命名模板
     * @return 重命名预览
     */
    public RenamePreviewDTO generateTvRename(VideoFileDTO videoFile,
                                             TmdbTvShowDTO tvInfo,
                                             TvTemplate template) {
        try {
            String newFileName = applyTvTemplate(template.getTemplate(), tvInfo, videoFile);
            return buildRenamePreview(videoFile, newFileName, null, tvInfo);
        } catch (Exception e) {
            log.error("生成电视剧重命名失败: {}", videoFile.getFilePath(), e);
            return buildErrorPreview(videoFile, e.getMessage());
        }
    }

    /**
     * 应用电影模板
     */
    private String applyMovieTemplate(String template, TmdbMovieDTO movie, VideoFileDTO videoFile) {
        String result = template;

        // 替换变量
        result = result.replace("{title}", sanitizeFileName(movie.getTitle()));
        result = result.replace("{year}", String.valueOf(movie.getYear()));
        result = result.replace("{ext}", videoFile.getExtension());

        // 可选: 分辨率 (需要从文件名提取)
        String resolution = extractResolution(videoFile.getFileName());
        result = result.replace("{resolution}", resolution != null ? resolution : "1080p");

        return result;
    }

    /**
     * 应用电视剧模板
     */
    private String applyTvTemplate(String template, TmdbTvShowDTO tvShow, VideoFileDTO videoFile) {
        String result = template;

        // 替换变量
        result = result.replace("{show}", sanitizeFileName(tvShow.getName()));
        result = result.replace("{year}", String.valueOf(tvShow.getYear()));
        result = result.replace("{season}", String.valueOf(videoFile.getParsedSeason()));
        result = result.replace("{episode}", String.valueOf(videoFile.getParsedEpisode()));
        result = result.replace("{ext}", videoFile.getExtension());

        // 格式化季集号 (S01E01)
        result = result.replaceAll(
                "\\{season:02d\\}",
                String.format("%02d", videoFile.getParsedSeason())
        );
        result = result.replaceAll(
                "\\{episode:02d\\}",
                String.format("%02d", videoFile.getParsedEpisode())
        );

        // 处理剧集标题 {title}
        String episodeTitle = "";
        if (template.contains("{title}")) {
            try {
                // 调用 TmdbService 获取该季度的所有剧集信息（会命中预取的缓存）
                List<com.mediarenamer.model.dto.TmdbEpisodeDTO> episodes =
                        tmdbService.getSeasonEpisodes(tvShow.getId(), videoFile.getParsedSeason());

                // 查找匹配当前集号的剧集
                if (episodes != null && !episodes.isEmpty()) {
                    for (com.mediarenamer.model.dto.TmdbEpisodeDTO episode : episodes) {
                        if (episode.getEpisodeNumber() != null &&
                                episode.getEpisodeNumber().equals(videoFile.getParsedEpisode())) {
                            // 找到匹配的集，获取标题并清洗
                            if (episode.getName() != null && !episode.getName().isEmpty()) {
                                episodeTitle = sanitizeFileName(episode.getName());
                                log.debug("✅ 标题: S{}E{} - {}",
                                        videoFile.getParsedSeason(),
                                        videoFile.getParsedEpisode(),
                                        episodeTitle);
                            }
                            break;
                        }
                    }
                }

                if (episodeTitle.isEmpty()) {
                    log.debug("⚠️ 未找到标题: tvId={}, S{}E{}",
                            tvShow.getId(),
                            videoFile.getParsedSeason(),
                            videoFile.getParsedEpisode());
                }
            } catch (Exception e) {
                log.warn("获取剧集标题失败: tvId={}, S{}E{}, error={}",
                        tvShow.getId(),
                        videoFile.getParsedSeason(),
                        videoFile.getParsedEpisode(),
                        e.getMessage());
            }
        }

        // 替换 {title}，如果为空则智能处理前导分隔符
        if (episodeTitle.isEmpty()) {
            // 移除 " - {title}" 这样的模式（包括前导分隔符）
            result = result.replaceAll("\\s*-\\s*\\{title\\}", "");
            // 兜底：直接替换 {title}
            result = result.replace("{title}", "");
        } else {
            result = result.replace("{title}", episodeTitle);
        }

        return result;
    }

    /**
     * 构建重命名预览
     *
     * @param videoFile 视频文件信息
     * @param newFileName 新文件名
     * @param movieInfo 电影信息（可为null）
     * @param tvInfo 电视剧信息（可为null）
     * @return 重命名预览
     */
    private RenamePreviewDTO buildRenamePreview(VideoFileDTO videoFile, String newFileName,
                                                TmdbMovieDTO movieInfo, TmdbTvShowDTO tvInfo) {
        File oldFile = new File(videoFile.getFilePath());

        // 使用扫描根目录作为基础路径
        String scanRoot = videoFile.getScanRootPath();
        if (scanRoot == null || scanRoot.isEmpty()) {
            // 兼容性处理：如果没有扫描根目录，回退到原逻辑
            scanRoot = oldFile.getParent();
            log.warn("未找到扫描根目录，使用原文件父目录: {}", scanRoot);
        }

        // 新文件的完整路径 = 扫描根目录 + 新文件名（包含子目录结构）
        String newPath = Paths.get(scanRoot, newFileName).toString();

        // 检查是否需要创建新目录（避免 NPE）
        File newFile = new File(newPath);
        File newParentDir = newFile.getParentFile();
        boolean needCreateDir = newParentDir != null && !newParentDir.exists();

        // ========== 计算新增的 4 个字段 ==========

        // 规范化扫描根目录路径（避免路径比较失败）
        Path scanRootPath = Paths.get(scanRoot).toAbsolutePath().normalize();
        Path oldFilePath = Paths.get(videoFile.getFilePath()).toAbsolutePath().normalize();
        Path oldParentPath = oldFilePath.getParent();

        // 1. 计算原相对目录路径
        String oldRelativeDirectory = calculateRelativeDirectory(scanRootPath, oldParentPath);

        // 2. 纯原文件名（不含路径）
        String pureOldFileName = videoFile.getFileName() + "." + videoFile.getExtension();

        // 3. 计算新相对目录路径
        Path newFilePath = Paths.get(newPath).toAbsolutePath().normalize();
        Path newParentPath = newFilePath.getParent();
        String newRelativeDirectory = calculateRelativeDirectory(scanRootPath, newParentPath);

        // 4. 纯新文件名（不含路径，仅文件名部分）
        String pureNewFileName = newFilePath.getFileName().toString();

        // ========== 构建 metadata 元数据 ==========
        Map<String, Object> metadata = new HashMap<>();

        if (movieInfo != null) {
            // 电影元数据
            metadata.put("mediaType", "MOVIE");
            metadata.put("groupKey", String.format("%s (%s)", movieInfo.getTitle(), movieInfo.getYear()));
            metadata.put("seriesName", movieInfo.getTitle());
        } else if (tvInfo != null && videoFile.getParsedSeason() != null) {
            // 电视剧元数据
            metadata.put("mediaType", "TV");

            String groupKey = String.format("%s (%s) - Season %d",
                    tvInfo.getName(), tvInfo.getYear(), videoFile.getParsedSeason());
            metadata.put("groupKey", groupKey);
            metadata.put("seriesName", tvInfo.getName());
            metadata.put("seasonNumber", videoFile.getParsedSeason());

            if (videoFile.getParsedEpisode() != null) {
                metadata.put("episodeNumber", videoFile.getParsedEpisode());
            }

            // 注意：季度总集数在批量预览时会被预取到缓存
            // 这里同步调用，但会命中缓存，速度很快
            try {
                Integer totalEpisodes = tmdbService.getSeasonEpisodeCount(
                        tvInfo.getId(), videoFile.getParsedSeason());
                if (totalEpisodes != null) {
                    metadata.put("seasonTotalEpisodes", totalEpisodes);
                }
            } catch (Exception e) {
                log.debug("获取季度总集数失败: tvId={}, season={}",
                        tvInfo.getId(), videoFile.getParsedSeason());
            }
        }

        return RenamePreviewDTO.builder()
                .oldPath(videoFile.getFilePath())
                .newPath(newPath)
                .oldFileName(videoFile.getFileName() + "." + videoFile.getExtension())
                .newFileName(newFileName)
                .oldRelativeDirectory(oldRelativeDirectory)
                .pureOldFileName(pureOldFileName)
                .newRelativeDirectory(newRelativeDirectory)
                .pureNewFileName(pureNewFileName)
                .needCreateDirectory(needCreateDir)
                .status("pending")
                .metadata(metadata)
                .build();
    }

    /**
     * 计算相对目录路径（提取公共逻辑，避免重复代码）
     *
     * @param scanRoot   扫描根目录
     * @param targetPath 目标路径
     * @return 相对路径字符串（如 "./" 或 "./subdir/"）
     */
    private String calculateRelativeDirectory(Path scanRoot, Path targetPath) {
        if (targetPath == null || targetPath.equals(scanRoot)) {
            return "./";
        }
        Path relativePath = scanRoot.relativize(targetPath);
        return "./" + relativePath.toString().replace("\\", "/") + "/";
    }

    /**
     * 构建错误预览
     */
    private RenamePreviewDTO buildErrorPreview(VideoFileDTO videoFile, String errorMessage) {
        return RenamePreviewDTO.builder()
                .oldPath(videoFile.getFilePath())
                .oldFileName(videoFile.getFileName() + "." + videoFile.getExtension())
                .status("failed")
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 批量执行重命名 - 使用虚拟线程并行处理
     *
     * @param previews 重命名预览列表
     * @param scanRoot 扫描根目录 (可选, 如果提供则会自动整理未处理文件)
     * @return 执行结果列表
     */
    public List<RenamePreviewDTO> executeRename(List<RenamePreviewDTO> previews, String scanRoot) {
        log.info("开始批量重命名，总数: {}", previews.size());
        long startTime = System.currentTimeMillis();

        // 过滤出需要处理的任务（跳过已失败的）
        List<RenamePreviewDTO> validPreviews = previews.stream()
                .filter(p -> !"failed".equals(p.getStatus()))
                .toList();

        // 已失败的任务直接添加到结果中
        List<RenamePreviewDTO> failedPreviews = previews.stream()
                .filter(p -> "failed".equals(p.getStatus()))
                .toList();

        // 使用线程安全的集合存储结果
        List<RenamePreviewDTO> results = Collections.synchronizedList(new ArrayList<>(failedPreviews));

        if (validPreviews.isEmpty()) {
            log.info("没有需要重命名的文件");
            return results;
        }

        // 使用虚拟线程执行器 (JDK 21+)
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // 提交所有重命名任务
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (RenamePreviewDTO preview : validPreviews) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        // 执行单个文件重命名
                        boolean success = renameSingleFile(preview);
                        if (success) {
                            preview.setStatus("success");
                            log.info("重命名成功: {} -> {}", preview.getOldFileName(), preview.getNewFileName());
                        } else {
                            preview.setStatus("failed");
                            preview.setErrorMessage("重命名失败");
                            log.error("重命名失败: {}", preview.getOldPath());
                        }
                    } catch (Exception e) {
                        preview.setStatus("failed");
                        preview.setErrorMessage(e.getMessage());
                        log.error("重命名异常: {}", preview.getOldPath(), e);
                    }
                    results.add(preview);
                }, executor);

                futures.add(future);
            }

            // 等待所有任务完成
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            // 等待完成（无超时限制，因为文件操作可能很慢）
            allOf.join();

        } catch (Exception e) {
            log.error("批量重命名失败", e);
            throw new RuntimeException("批量重命名失败", e);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        long successCount = results.stream().filter(r -> "success".equals(r.getStatus())).count();
        long failedCount = results.stream().filter(r -> "failed".equals(r.getStatus())).count();

        log.info("批量重命名完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                results.size(), successCount, failedCount, elapsedTime);

        // 如果提供了扫描根目录，自动整理未处理文件
        if (scanRoot != null && !scanRoot.isEmpty() && successCount > 0) {
            try {
                log.info("🗂️  开始自动整理未处理文件...");
                organizationService.organizeUnprocessedFiles(scanRoot, results);
                log.info("✅ 自动整理完成");
            } catch (Exception e) {
                log.error("⚠️  自动整理失败: {}", e.getMessage(), e);
                // 整理失败不影响重命名结果，只记录警告
            }
        }

        return results;
    }

    /**
     * 执行单个文件重命名（线程安全）
     */
    private boolean renameSingleFile(RenamePreviewDTO preview) {
        try {
            File oldFile = new File(preview.getOldPath());
            File newFile = new File(preview.getNewPath());

            // 检查原文件是否存在
            if (!oldFile.exists()) {
                log.error("原文件不存在: {}", preview.getOldPath());
                return false;
            }

            // 检查源文件和目标文件是否是同一个文件（路径相同）
            if (oldFile.getAbsolutePath().equals(newFile.getAbsolutePath())) {
                log.debug("源文件和目标文件相同，跳过重命名: {}", preview.getOldPath());
                return true;  // 跳过，视为成功
            }

            // 创建目标目录（线程安全）
            if (preview.getNeedCreateDirectory()) {
                File parentDir = newFile.getParentFile();
                String dirPath = parentDir.getAbsolutePath();

                // 使用目录路径作为锁，确保同一目录只被创建一次
                Object lock = directoryLocks.computeIfAbsent(dirPath, k -> new Object());

                synchronized (lock) {
                    if (!parentDir.exists()) {
                        boolean created = parentDir.mkdirs();
                        if (!created) {
                            log.error("创建目录失败: {}", dirPath);
                            return false;
                        }
                        log.info("创建目录成功: {}", dirPath);
                    }
                }
            }

            // 检查目标文件是否已存在
            if (newFile.exists()) {
                log.error("目标文件已存在: {}", preview.getNewPath());
                return false;
            }

            // 执行文件移动（支持跨分区）
            return moveFile(oldFile, newFile);
        } catch (Exception e) {
            log.error("移动文件失败: {}", preview.getOldPath(), e);
            return false;
        }
    }

    /**
     * 移动文件（支持跨分区移动）
     *
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     * @return 是否成功
     */
    private boolean moveFile(File sourceFile, File targetFile) {
        try {
            // 尝试直接重命名（同分区，速度快）
            if (sourceFile.renameTo(targetFile)) {
                log.debug("文件移动成功（直接重命名）: {} -> {}", sourceFile.getName(), targetFile.getAbsolutePath());
                return true;
            }

            // 重命名失败，可能是跨分区，使用复制+删除方式
            log.info("直接重命名失败，使用复制+删除方式: {}", sourceFile.getAbsolutePath());
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 验证复制是否成功
            if (!targetFile.exists() || targetFile.length() != sourceFile.length()) {
                log.error("文件复制验证失败: 大小不匹配");
                // 清理可能的不完整文件
                if (targetFile.exists()) {
                    targetFile.delete();
                }
                return false;
            }

            // 删除原文件
            boolean deleted = sourceFile.delete();
            if (!deleted) {
                log.warn("原文件删除失败（目标文件已创建）: {}", sourceFile.getAbsolutePath());
                // 即使删除失败，目标文件已创建成功，可以认为移动成功
            }

            log.info("文件移动成功（复制+删除）: {} -> {}", sourceFile.getName(), targetFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            log.error("文件移动失败: {} -> {}", sourceFile, targetFile, e);
            return false;
        }
    }

    /**
     * 从文件名提取分辨率
     */
    private String extractResolution(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.contains("2160p") || lowerName.contains("4k")) {
            return "2160p";
        } else if (lowerName.contains("1080p")) {
            return "1080p";
        } else if (lowerName.contains("720p")) {
            return "720p";
        } else if (lowerName.contains("480p")) {
            return "480p";
        }
        return null;
    }

    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        // 替换非法字符: \ / : * ? " < > |
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "");
    }

    /**
     * 批量生成重命名预览
     * 使用虚拟线程并发处理，大幅提升性能
     *
     * 优化策略:
     * 1. 预取所有季度信息到缓存 (减少重复 API 调用)
     * 2. 并行生成预览 (利用虚拟线程)
     *
     * @param request 批量请求
     * @return 批量响应（包含预览列表和统计信息）
     */
    public com.mediarenamer.controller.RenameController.BatchRenameResponse batchGeneratePreview(
            com.mediarenamer.controller.RenameController.BatchRenameRequest request) {
        log.info("开始批量生成重命名预览，总数: {}", request.getRequests().size());
        long startTime = System.currentTimeMillis();

        // ========== 优化：预取所有季度信息 ==========
        prefetchSeasonInfo(request.getRequests());

        // 使用线程安全的集合存储结果
        List<RenamePreviewDTO> results = Collections.synchronizedList(new ArrayList<>());

        // 使用虚拟线程执行器 (JDK 21+)
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // 提交所有预览生成任务
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (com.mediarenamer.controller.RenameController.SingleRenameRequest req : request.getRequests()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        RenamePreviewDTO preview = generateSinglePreview(req);
                        results.add(preview);
                    } catch (Exception e) {
                        log.error("生成预览失败: file={}", req.getVideoFile().getFilePath(), e);
                        // 生成错误预览
                        RenamePreviewDTO errorPreview = buildErrorPreview(req.getVideoFile(), e.getMessage());
                        results.add(errorPreview);
                    }
                }, executor);

                futures.add(future);
            }

            // 等待所有任务完成（5分钟超时）
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            try {
                allOf.get(5, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                log.error("批量生成预览超时");
                throw new RuntimeException("批量生成预览超时", e);
            } catch (InterruptedException | ExecutionException e) {
                log.error("批量生成预览失败", e);
                throw new RuntimeException("批量生成预览失败", e);
            }

        } catch (Exception e) {
            log.error("批量生成预览异常", e);
            throw new RuntimeException("批量生成预览失败", e);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        // 构建响应
        return buildBatchPreviewResponse(results, elapsedTime);
    }

    /**
     * 生成单个文件的重命名预览
     *
     * @param request 单个请求
     * @return 重命名预览
     */
    private RenamePreviewDTO generateSinglePreview(
            com.mediarenamer.controller.RenameController.SingleRenameRequest request) {
        try {
            String mediaType = request.getMediaType();
            if (mediaType == null) {
                mediaType = request.getVideoFile().getMediaType();
            }

            if ("MOVIE".equals(mediaType)) {
                // 将 Object 转换为 TmdbMovieDTO
                TmdbMovieDTO movieInfo = convertToMovieDTO(request.getMatchedInfo());
                MovieTemplate template = MovieTemplate.valueOf(request.getTemplate());
                return generateMovieRename(request.getVideoFile(), movieInfo, template);
            } else if ("TV_SHOW".equals(mediaType)) {
                // 将 Object 转换为 TmdbTvShowDTO
                TmdbTvShowDTO tvInfo = convertToTvShowDTO(request.getMatchedInfo());
                TvTemplate template = TvTemplate.valueOf(request.getTemplate());
                return generateTvRename(request.getVideoFile(), tvInfo, template);
            } else {
                throw new RuntimeException("未知媒体类型: " + mediaType);
            }
        } catch (Exception e) {
            log.error("生成单个预览失败", e);
            return buildErrorPreview(request.getVideoFile(), e.getMessage());
        }
    }

    /**
     * 构建批量预览响应
     *
     * @param results 预览结果列表
     * @param durationMs 耗时（毫秒）
     * @return 批量响应
     */
    private com.mediarenamer.controller.RenameController.BatchRenameResponse buildBatchPreviewResponse(
            List<RenamePreviewDTO> results, long durationMs) {
        int total = results.size();
        int success = (int) results.stream().filter(r -> !"failed".equals(r.getStatus())).count();
        int failed = total - success;

        com.mediarenamer.controller.RenameController.BatchSummary summary =
                com.mediarenamer.controller.RenameController.BatchSummary.builder()
                        .total(total)
                        .success(success)
                        .failed(failed)
                        .durationMs(durationMs)
                        .build();

        return com.mediarenamer.controller.RenameController.BatchRenameResponse.builder()
                .previews(results)
                .summary(summary)
                .build();
    }

    /**
     * 将 Object 转换为 TmdbMovieDTO
     */
    private TmdbMovieDTO convertToMovieDTO(Object obj) {
        if (obj instanceof TmdbMovieDTO) {
            return (TmdbMovieDTO) obj;
        }
        // 如果是 Map，可以通过 Jackson 转换
        if (obj instanceof java.util.Map) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.convertValue(obj, TmdbMovieDTO.class);
        }
        throw new RuntimeException("无法转换为 TmdbMovieDTO");
    }

    /**
     * 将 Object 转换为 TmdbTvShowDTO
     */
    private TmdbTvShowDTO convertToTvShowDTO(Object obj) {
        if (obj instanceof TmdbTvShowDTO) {
            return (TmdbTvShowDTO) obj;
        }
        // 如果是 Map，可以通过 Jackson 转换
        if (obj instanceof java.util.Map) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.convertValue(obj, TmdbTvShowDTO.class);
        }
        throw new RuntimeException("无法转换为 TmdbTvShowDTO");
    }

    /**
     * 预取季度剧集信息到缓存
     * 批量预取可以避免后续并行任务中的重复 API 调用
     *
     * 优化要点:
     * 1. 直接预取 getSeasonEpisodes（包含标题），而不是 getSeasonEpisodeCount
     * 2. 使用虚拟线程并行预取，提升性能
     * 3. 预取完成后，后续任务直接命中缓存，无需网络请求
     *
     * @param requests 请求列表
     */
    private void prefetchSeasonInfo(List<com.mediarenamer.controller.RenameController.SingleRenameRequest> requests) {
        log.debug("开始预取季度剧集信息");
        long startTime = System.currentTimeMillis();

        // 收集所有需要预取的 (tvId, seasonNumber) 组合
        Set<String> uniqueSeasons = new HashSet<>();
        for (com.mediarenamer.controller.RenameController.SingleRenameRequest req : requests) {
            if ("TV_SHOW".equals(req.getMediaType()) && req.getVideoFile().getParsedSeason() != null) {
                try {
                    TmdbTvShowDTO tvInfo = convertToTvShowDTO(req.getMatchedInfo());
                    String key = tvInfo.getId() + "-" + req.getVideoFile().getParsedSeason();
                    uniqueSeasons.add(key);
                } catch (Exception e) {
                    log.debug("解析 TV 信息失败: {}", e.getMessage());
                }
            }
        }

        if (uniqueSeasons.isEmpty()) {
            log.debug("无需预取季度剧集信息（无电视剧）");
            return;
        }

        log.info("🚀 开始预取季度剧集信息: 总数={}", uniqueSeasons.size());

        // 并行预取所有季度的剧集详情（包含标题）
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (String key : uniqueSeasons) {
                String[] parts = key.split("-");
                Long tvId = Long.parseLong(parts[0]);
                Integer seasonNumber = Integer.parseInt(parts[1]);

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        // 关键优化：调用 getSeasonEpisodes 预取标题数据
                        // 这样后续的 applyTvTemplate 就能直接命中缓存
                        tmdbService.getSeasonEpisodes(tvId, seasonNumber);
                        log.debug("✅ 预取成功: tvId={}, season={}", tvId, seasonNumber);
                    } catch (Exception e) {
                        // 异常已在 TmdbService 中处理，这里只记录 DEBUG
                        log.debug("预取失败: tvId={}, season={}", tvId, seasonNumber);
                    }
                }, executor);

                futures.add(future);
            }

            // 等待所有预取完成 (30秒超时)
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("预取季度剧集信息超时（部分成功）");
            } catch (Exception e) {
                log.warn("预取季度剧集信息异常: {}", e.getMessage());
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("✅ 预取季度剧集信息完成: 总数={}, 耗时={}ms", uniqueSeasons.size(), elapsedTime);
    }

}
