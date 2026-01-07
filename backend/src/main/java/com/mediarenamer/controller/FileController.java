package com.mediarenamer.controller;

import com.mediarenamer.model.Result;
import com.mediarenamer.model.dto.DirectoryNode;
import com.mediarenamer.model.dto.VideoFileDTO;
import com.mediarenamer.service.FileScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件扫描控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileScanner fileScanner;

    /**
     * 扫描目录
     *
     * @param directory 目录路径
     * @param mediaType 媒体类型 (可选): MOVIE(电影) / TV_SHOW(电视剧) / MIXED(混合,默认)
     * @return 视频文件列表
     */
    @GetMapping("/scan")
    public Result<List<VideoFileDTO>> scanDirectory(
            @RequestParam String directory,
            @RequestParam(required = false) String mediaType) {
        try {
            log.info("接收扫描请求: directory={}, mediaType={}", directory, mediaType);
            List<VideoFileDTO> files = fileScanner.scanDirectory(directory, mediaType);
            return Result.success("扫描成功", files);
        } catch (Exception e) {
            log.error("扫描目录失败: {}", directory, e);
            return Result.error("扫描失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定路径下的子目录列表
     * 用于前端目录选择器的懒加载
     *
     * @param path 父级目录路径（可选，为空时返回系统根节点）
     * @return 子目录列表
     */
    @GetMapping("/directories")
    public Result<List<DirectoryNode>> listDirectories(@RequestParam(required = false) String path) {
        try {
            log.info("获取目录列表: path={}", path);

            List<DirectoryNode> directories;

            if (!StringUtils.hasText(path)) {
                // 返回系统根节点
                directories = getRootDirectories();
            } else {
                // 返回指定路径的子目录
                directories = getSubDirectories(path);
            }

            return Result.success("获取目录列表成功", directories);
        } catch (Exception e) {
            log.error("获取目录列表失败: path={}", path, e);
            return Result.error("获取目录列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统根目录节点
     * Windows: 返回所有盘符（C:\, D:\ 等）
     * macOS/Linux: 返回用户主目录和根目录
     */
    private List<DirectoryNode> getRootDirectories() {
        List<DirectoryNode> roots = new ArrayList<>();
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            // Windows: 列出所有盘符
            File[] drives = File.listRoots();
            if (drives != null) {
                for (File drive : drives) {
                    roots.add(DirectoryNode.builder()
                            .name(drive.getPath())
                            .path(drive.getAbsolutePath())
                            .hasChildren(hasSubDirectories(drive.toPath()))
                            .isWritable(drive.canWrite())
                            .build());
                }
            }
        } else {
            // Unix-like: 提供用户主目录和根目录作为起点
            String userHome = System.getProperty("user.home");
            Path userHomePath = Paths.get(userHome);
            roots.add(DirectoryNode.builder()
                    .name("主目录 (" + userHomePath.getFileName() + ")")
                    .path(userHome)
                    .hasChildren(hasSubDirectories(userHomePath))
                    .isWritable(Files.isWritable(userHomePath))
                    .build());

            Path rootPath = Paths.get("/");
            roots.add(DirectoryNode.builder()
                    .name("根目录 (/)")
                    .path("/")
                    .hasChildren(hasSubDirectories(rootPath))
                    .isWritable(Files.isWritable(rootPath))
                    .build());
        }

        return roots;
    }

    /**
     * 获取指定路径下的子目录
     */
    private List<DirectoryNode> getSubDirectories(String parentPath) throws IOException {
        Path path = Paths.get(parentPath);

        // 验证路径是否存在且为目录
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("路径不存在: " + parentPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("路径不是目录: " + parentPath);
        }

        // 列出子目录（过滤掉文件和隐藏文件夹）
        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(p -> !isHidden(p))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .map(p -> DirectoryNode.builder()
                            .name(p.getFileName().toString())
                            .path(p.toAbsolutePath().toString())
                            .hasChildren(hasSubDirectories(p))
                            .isWritable(Files.isWritable(p))
                            .build())
                    .collect(Collectors.toList());
        }
    }

    /**
     * 检查目录是否包含子目录
     */
    private boolean hasSubDirectories(Path path) {
        try (Stream<Path> stream = Files.list(path)) {
            return stream.anyMatch(p -> Files.isDirectory(p) && !isHidden(p));
        } catch (IOException e) {
            // 无权限或其他错误，假设没有子目录
            return false;
        }
    }

    /**
     * 判断是否为隐藏文件夹
     * Unix-like: 以 . 开头
     * Windows: 使用 Files.isHidden
     */
    private boolean isHidden(Path path) {
        try {
            String fileName = path.getFileName().toString();
            // Unix-like 隐藏文件判断
            if (fileName.startsWith(".")) {
                return true;
            }
            // Windows 隐藏文件判断
            return Files.isHidden(path);
        } catch (IOException e) {
            return false;
        }
    }

}
