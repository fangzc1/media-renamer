package com.mediarenamer.service;

import com.mediarenamer.model.dto.RenamePreviewDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文件组织服务
 * 负责将未整理的文件移动到 "未整理" 目录
 */
@Slf4j
@Service
public class OrganizationService {

    private static final String UNORGANIZED_DIR_NAME = "未整理";

    /**
     * 整理未处理的文件
     * 1. 移动扫描根目录下的残留文件/目录到"未整理"目录
     * 2. 清理所有识别到的媒体目录内部的原始文件/目录
     *
     * @param scanRoot 扫描根目录
     * @param renameResults 重命名结果列表
     */
    public void organizeUnprocessedFiles(String scanRoot, List<RenamePreviewDTO> renameResults) {
        log.info("开始整理未处理文件，扫描根目录: {}", scanRoot);

        try {
            // 1. 创建 "未整理" 目录
            File unorganizedDir = new File(scanRoot, UNORGANIZED_DIR_NAME);
            if (!unorganizedDir.exists()) {
                boolean created = unorganizedDir.mkdirs();
                if (!created) {
                    log.error("创建未整理目录失败: {}", unorganizedDir.getAbsolutePath());
                    return;
                }
                log.info("创建未整理目录: {}", unorganizedDir.getAbsolutePath());
            }

            // 2. 识别所有媒体目录 (不依赖 renameResults，基于命名规范识别)
            Set<String> allMediaDirs = findAllMediaDirectories(scanRoot);
            log.info("识别到的媒体目录: {}", allMediaDirs);

            // 3. 整理扫描根目录下的残留文件/目录
            int rootLevelMoved = organizeRootLevel(scanRoot, unorganizedDir, allMediaDirs);

            // 4. 清理所有识别到的媒体目录内部的原始残留内容
            int innerLevelMoved = cleanupAllMediaDirectories(scanRoot, allMediaDirs, unorganizedDir);

            log.info("整理完成，移动 {} 个文件/目录到未整理目录（根目录: {}, 媒体目录内部: {}）",
                    rootLevelMoved + innerLevelMoved, rootLevelMoved, innerLevelMoved);

        } catch (Exception e) {
            log.error("整理未处理文件失败", e);
            throw new RuntimeException("整理未处理文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 识别扫描根目录下所有符合媒体命名规范的目录
     * 规范: "片名 (年份)" 格式，例如：小猪佩奇 (2004)、Breaking Bad (2008)
     *
     * @param scanRoot 扫描根目录
     * @return 媒体目录名称集合
     */
    private Set<String> findAllMediaDirectories(String scanRoot) {
        Set<String> mediaDirs = new HashSet<>();
        File scanRootDir = new File(scanRoot);
        File[] entries = scanRootDir.listFiles();

        if (entries == null) {
            log.warn("扫描根目录为空或无法访问: {}", scanRoot);
            return mediaDirs;
        }

        for (File entry : entries) {
            // 跳过隐藏文件和"未整理"目录
            if (entry.isHidden() || entry.getName().startsWith(".") ||
                    entry.getName().equals(UNORGANIZED_DIR_NAME)) {
                continue;
            }

            if (entry.isDirectory() && isMediaDirectory(entry.getName())) {
                mediaDirs.add(entry.getName());
                log.debug("识别为媒体目录: {}", entry.getName());
            }
        }

        return mediaDirs;
    }

    /**
     * 判断是否是媒体目录
     * 匹配格式: "片名 (年份)" 或 "片名 (年份) - 额外信息"
     *
     * @param dirName 目录名称
     * @return 是否是媒体目录
     */
    private boolean isMediaDirectory(String dirName) {
        // 匹配 "xxx (2004)" 或 "xxx (2004) - xxx" 格式
        // 正则: 任意字符 + (4位数字) + 任意字符
        return dirName.matches(".*\\(\\d{4}\\).*");
    }

    /**
     * 整理扫描根目录下的一级文件/目录
     *
     * @param scanRoot 扫描根目录
     * @param unorganizedDir 未整理目录
     * @param newMediaDirNames 新创建的媒体目录名称集合
     * @return 移动的文件/目录数量
     */
    private int organizeRootLevel(String scanRoot, File unorganizedDir, Set<String> newMediaDirNames) {
        File scanRootDir = new File(scanRoot);
        File[] entries = scanRootDir.listFiles();
        if (entries == null) {
            log.warn("扫描根目录为空或无法访问: {}", scanRoot);
            return 0;
        }

        int movedCount = 0;
        for (File entry : entries) {
            // 跳过隐藏文件和 "未整理" 目录本身
            if (entry.isHidden() || entry.getName().startsWith(".") ||
                    entry.getName().equals(UNORGANIZED_DIR_NAME)) {
                continue;
            }

            // 判断是否需要移动
            boolean shouldMove = false;

            if (entry.isDirectory()) {
                // 目录：只保留新创建的媒体目录
                if (!newMediaDirNames.contains(entry.getName())) {
                    shouldMove = true;
                    log.debug("根目录未处理目录: {}", entry.getName());
                }
            } else if (entry.isFile()) {
                // 文件：全部移动
                shouldMove = true;
                log.debug("根目录未处理文件: {}", entry.getName());
            }

            if (shouldMove) {
                moveToUnorganized(entry, unorganizedDir);
                movedCount++;
            }
        }

        return movedCount;
    }

    /**
     * 清理所有识别到的媒体目录内部的原始残留内容
     * 只保留新生成的 Season XX 格式目录和视频文件
     *
     * @param scanRoot 扫描根目录
     * @param allMediaDirs 所有识别到的媒体目录名称集合
     * @param unorganizedDir 未整理目录
     * @return 移动的文件/目录数量
     */
    private int cleanupAllMediaDirectories(String scanRoot, Set<String> allMediaDirs, File unorganizedDir) {
        int totalMoved = 0;

        for (String mediaDirName : allMediaDirs) {
            File mediaDir = new File(scanRoot, mediaDirName);
            if (!mediaDir.exists() || !mediaDir.isDirectory()) {
                continue;
            }

            // 为每个媒体目录创建对应的"未整理"子目录
            File mediaUnorganizedDir = new File(unorganizedDir, mediaDirName);
            if (!mediaUnorganizedDir.exists()) {
                mediaUnorganizedDir.mkdirs();
            }

            File[] entries = mediaDir.listFiles();
            if (entries == null) {
                continue;
            }

            int mediaMoved = 0;
            for (File entry : entries) {
                // 跳过隐藏文件
                if (entry.isHidden() || entry.getName().startsWith(".")) {
                    continue;
                }

                boolean shouldMove = false;

                if (entry.isDirectory()) {
                    // 保留新格式的 Season 目录 (Season 01, Season 02, ...)
                    // 移动旧格式的 Season 目录 (Season 1, Season 2, ...)
                    String dirName = entry.getName();
                    if (dirName.matches("Season \\d{2}")) {
                        // 新格式 Season 目录，保留
                        log.debug("保留新格式 Season 目录: {}/{}", mediaDirName, dirName);
                    } else {
                        // 其他目录，移动
                        shouldMove = true;
                        log.debug("媒体目录内未处理目录: {}/{}", mediaDirName, dirName);
                    }
                } else if (entry.isFile()) {
                    // 保留视频文件，移动其他文件（图片、nfo等）
                    String fileName = entry.getName().toLowerCase();
                    if (fileName.endsWith(".mkv") || fileName.endsWith(".mp4") ||
                            fileName.endsWith(".avi") || fileName.endsWith(".ts") ||
                            fileName.endsWith(".m2ts")) {
                        // 视频文件，保留
                        log.debug("保留视频文件: {}/{}", mediaDirName, entry.getName());
                    } else {
                        // 其他文件（图片、nfo等），移动
                        shouldMove = true;
                        log.debug("媒体目录内未处理文件: {}/{}", mediaDirName, entry.getName());
                    }
                }

                if (shouldMove) {
                    moveToUnorganized(entry, mediaUnorganizedDir);
                    mediaMoved++;
                    totalMoved++;
                }
            }

            if (mediaMoved > 0) {
                log.info("清理 {} 完成: 移动 {} 个文件/目录", mediaDirName, mediaMoved);
            }
        }

        return totalMoved;
    }

    /**
     * 从重命名结果中提取媒体目录名称（一级子目录）
     *
     * @param scanRoot 扫描根目录
     * @param renameResults 重命名结果
     * @return 媒体目录名称集合
     */
    private Set<String> extractMediaDirectories(String scanRoot, List<RenamePreviewDTO> renameResults) {
        Set<String> mediaDirs = new HashSet<>();

        for (RenamePreviewDTO result : renameResults) {
            if (!"success".equals(result.getStatus())) {
                continue;
            }

            String newPath = result.getNewPath();
            if (newPath == null || newPath.isEmpty()) {
                continue;
            }

            try {
                // 获取新路径相对于扫描根目录的路径
                Path scanRootPath = Paths.get(scanRoot).toAbsolutePath().normalize();
                Path newFilePath = Paths.get(newPath).toAbsolutePath().normalize();

                // 计算相对路径
                Path relativePath = scanRootPath.relativize(newFilePath);

                // 提取第一级目录名称（电影名/剧集名）
                if (relativePath.getNameCount() > 0) {
                    String firstLevelDir = relativePath.getName(0).toString();
                    mediaDirs.add(firstLevelDir);
                }
            } catch (Exception e) {
                log.warn("提取媒体目录失败: {}", newPath, e);
            }
        }

        return mediaDirs;
    }

    /**
     * 将文件或目录移动到未整理目录
     *
     * @param source 源文件或目录
     * @param unorganizedDir 未整理目录
     */
    private void moveToUnorganized(File source, File unorganizedDir) {
        try {
            File target = new File(unorganizedDir, source.getName());

            // 检查目标是否已存在
            if (target.exists()) {
                log.warn("目标已存在，跳过移动: {}", target.getAbsolutePath());
                return;
            }

            // 尝试直接移动
            if (source.renameTo(target)) {
                log.info("移动成功: {} -> {}", source.getName(), target.getAbsolutePath());
                return;
            }

            // 移动失败，使用复制+删除（处理目录或跨分区情况）
            if (source.isDirectory()) {
                FileUtils.moveDirectoryToDirectory(source, unorganizedDir, true);
                log.info("移动目录成功: {} -> {}", source.getName(), target.getAbsolutePath());
            } else {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                boolean deleted = source.delete();
                if (!deleted) {
                    log.warn("原文件删除失败: {}", source.getAbsolutePath());
                }
                log.info("移动文件成功: {} -> {}", source.getName(), target.getAbsolutePath());
            }

        } catch (IOException e) {
            log.error("移动失败: {}", source.getAbsolutePath(), e);
        }
    }
}
