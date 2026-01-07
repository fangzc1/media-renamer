package com.mediarenamer.service;

import com.mediarenamer.config.FileProperties;
import com.mediarenamer.model.dto.VideoFileDTO;
import com.mediarenamer.parser.ParseResult;
import com.mediarenamer.parser.ParsingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * 文件扫描服务 (重构版)
 *
 * 负责扫描目录、识别视频文件、解析文件名
 *
 * 架构改进:
 * - 移除所有硬编码的正则表达式
 * - 使用插件化的解析器架构
 * - 简化代码逻辑,提高可维护性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileScanner {

    private final FileProperties fileProperties;
    private final MediaParsingService mediaParsingService;

    /**
     * 扫描目录,获取所有视频文件 (并行优化版)
     *
     * @param directoryPath 目录路径
     * @param mediaType 媒体类型 (可选): MOVIE(电影) / TV_SHOW(电视剧) / MIXED(混合) / null(默认为混合)
     * @return 视频文件列表
     */
    public List<VideoFileDTO> scanDirectory(String directoryPath, String mediaType) {
        log.info("开始扫描目录: {}, 媒体类型: {}", directoryPath, mediaType);
        long startTime = System.currentTimeMillis();

        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                throw new RuntimeException("目录不存在: " + directoryPath);
            }

            if (!Files.isDirectory(path)) {
                throw new RuntimeException("不是有效的目录: " + directoryPath);
            }

            // 保存扫描根目录的绝对路径
            final String scanRoot = path.toAbsolutePath().toString();

            // 使用线程安全的集合存储结果
            List<VideoFileDTO> videoFiles = Collections.synchronizedList(new ArrayList<>());

            // 使用虚拟线程并行扫描目录
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                scanDirectoryParallel(path.toFile(), videoFiles, 0, scanRoot, mediaType, executor)
                        .join(); // 等待所有任务完成
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("扫描完成,共找到 {} 个视频文件, 耗时: {}ms", videoFiles.size(), elapsedTime);
            return videoFiles;
        } catch (Exception e) {
            log.error("扫描目录失败: {}", directoryPath, e);
            throw new RuntimeException("扫描目录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 并行扫描目录 (使用虚拟线程)
     *
     * @param directory 目录
     * @param videoFiles 结果列表 (线程安全)
     * @param depth 当前深度
     * @param scanRoot 扫描根目录
     * @param mediaType 媒体类型
     * @param executor 虚拟线程执行器
     * @return CompletableFuture
     */
    private CompletableFuture<Void> scanDirectoryParallel(File directory, List<VideoFileDTO> videoFiles,
                                                          int depth, String scanRoot, String mediaType,
                                                          ExecutorService executor) {
        // 防止递归过深
        if (depth > fileProperties.getMaxScanDepth()) {
            log.warn("目录深度超过限制,跳过: {}", directory.getAbsolutePath());
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            File[] files = directory.listFiles();
            if (files == null) {
                return;
            }

            // 收集所有子任务
            List<CompletableFuture<Void>> subTasks = new ArrayList<>();

            for (File file : files) {
                // 跳过隐藏文件和目录
                if (file.isHidden() || file.getName().startsWith(".")) {
                    continue;
                }

                if (file.isDirectory()) {
                    // 递归扫描子目录 (并行)
                    CompletableFuture<Void> subTask = scanDirectoryParallel(
                            file, videoFiles, depth + 1, scanRoot, mediaType, executor);
                    subTasks.add(subTask);
                } else if (file.isFile() && isVideoFile(file)) {
                    // 并行解析视频文件
                    CompletableFuture<Void> parseTask = CompletableFuture.runAsync(() -> {
                        VideoFileDTO videoFile = parseVideoFile(file, scanRoot, mediaType);
                        videoFiles.add(videoFile);
                    }, executor);
                    subTasks.add(parseTask);
                }
            }

            // 等待所有子任务完成
            if (!subTasks.isEmpty()) {
                CompletableFuture.allOf(subTasks.toArray(new CompletableFuture[0])).join();
            }
        }, executor);
    }

    /**
     * 递归扫描目录 (保留原方法作为备用)
     */
    private void scanDirectoryRecursive(File directory, List<VideoFileDTO> videoFiles,
                                       int depth, String scanRoot, String mediaType) {
        // 防止递归过深
        if (depth > fileProperties.getMaxScanDepth()) {
            log.warn("目录深度超过限制,跳过: {}", directory.getAbsolutePath());
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            // 跳过隐藏文件和目录
            if (file.isHidden() || file.getName().startsWith(".")) {
                continue;
            }

            if (file.isDirectory()) {
                // 递归扫描子目录
                scanDirectoryRecursive(file, videoFiles, depth + 1, scanRoot, mediaType);
            } else if (file.isFile() && isVideoFile(file)) {
                // 解析视频文件
                VideoFileDTO videoFile = parseVideoFile(file, scanRoot, mediaType);
                videoFiles.add(videoFile);
            }
        }
    }

    /**
     * 判断是否为视频文件
     */
    private boolean isVideoFile(File file) {
        String fileName = file.getName().toLowerCase();
        for (String ext : fileProperties.getVideoExtensions()) {
            if (fileName.endsWith("." + ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析视频文件信息 (重构版)
     *
     * @param file 文件对象
     * @param scanRoot 扫描根目录
     * @param preferredMediaType 用户预设的媒体类型 (MOVIE/TV_SHOW/MIXED/null)
     */
    private VideoFileDTO parseVideoFile(File file, String scanRoot, String preferredMediaType) {
        String fullPath = file.getAbsolutePath();
        String fileName = file.getName();
        String fileNameWithoutExt = getFileNameWithoutExtension(fileName);
        String extension = getFileExtension(fileName);
        long fileSize = file.length();

        // 获取父目录和祖父目录信息
        File parent = file.getParentFile();
        String parentDirName = null;
        String grandParentDirName = null;

        if (parent != null) {
            parentDirName = parent.getName();

            // 获取祖父目录（确保不是扫描根目录）
            File grandParent = parent.getParentFile();
            if (grandParent != null && !grandParent.getAbsolutePath().equals(scanRoot)) {
                grandParentDirName = grandParent.getName();
            }
        }

        // 智能推断剧集名称
        String detectedSeriesName = mediaParsingService.detectSeriesName(file, scanRoot);

        // 判断父目录是否为季目录
        boolean parentIsSeasonFolder = mediaParsingService.isParentDirectorySeasonFolder(parentDirName);

        // 构建解析上下文
        ParsingContext context = ParsingContext.builder()
                .fileName(fileName)
                .fileNameWithoutExt(fileNameWithoutExt)
                .extension(extension)
                .parentDirectory(parentDirName)
                .grandParentDirectory(grandParentDirName)
                .preferredMediaType(preferredMediaType)
                .scanRootPath(scanRoot)
                .parentIsSeasonFolder(parentIsSeasonFolder)
                .detectedSeriesName(detectedSeriesName)
                .build();

        // 使用新的解析服务
        ParseResult parseResult = mediaParsingService.parseMediaFile(context);

        // 构建 DTO
        VideoFileDTO dto = VideoFileDTO.builder()
                .id(UUID.randomUUID().toString())
                .filePath(fullPath)
                .fileName(fileNameWithoutExt)
                .extension(extension)
                .fileSize(fileSize)
                .fileSizeReadable(FileUtils.byteCountToDisplaySize(fileSize))
                .scanRootPath(scanRoot)
                .parentDirectory(parentDirName)
                .grandParentDirectory(grandParentDirName)
                .mediaType(parseResult.getMediaType())
                .parsedTitle(parseResult.getTitle())
                .parsedYear(parseResult.getYear())
                .parsedSeason(parseResult.getSeason())
                .parsedEpisode(parseResult.getEpisode())
                .build();

        log.debug("文件解析完成: {} -> 类型={}, 标题={}, 置信度={}",
                fileName,
                parseResult.getMediaType(),
                parseResult.getTitle(),
                parseResult.getConfidence());

        return dto;
    }

    /**
     * 获取文件名 (不含扩展名)
     */
    private String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}
