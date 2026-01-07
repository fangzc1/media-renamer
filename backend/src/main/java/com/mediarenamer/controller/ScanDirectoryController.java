package com.mediarenamer.controller;

import com.mediarenamer.model.Result;
import com.mediarenamer.model.dto.ScanDirectoryDTO;
import com.mediarenamer.service.ScanDirectoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 扫描目录配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/scan-directories")
@RequiredArgsConstructor
public class ScanDirectoryController {

    private final ScanDirectoryService scanDirectoryService;

    /**
     * 获取所有扫描目录配置
     */
    @GetMapping
    public Result<List<ScanDirectoryDTO>> getAllDirectories() {
        try {
            List<ScanDirectoryDTO> directories = scanDirectoryService.getAllDirectories();
            return Result.success(directories);
        } catch (Exception e) {
            log.error("获取扫描目录配置失败", e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取启用的扫描目录
     */
    @GetMapping("/enabled")
    public Result<List<ScanDirectoryDTO>> getEnabledDirectories() {
        try {
            List<ScanDirectoryDTO> directories = scanDirectoryService.getEnabledDirectories();
            return Result.success(directories);
        } catch (Exception e) {
            log.error("获取启用的扫描目录失败", e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 添加扫描目录配置
     */
    @PostMapping
    public Result<ScanDirectoryDTO> addDirectory(@RequestBody ScanDirectoryDTO directory) {
        try {
            log.info("添加扫描目录: {} -> {}", directory.getName(), directory.getPath());
            ScanDirectoryDTO result = scanDirectoryService.addDirectory(directory);
            return Result.success("添加成功", result);
        } catch (Exception e) {
            log.error("添加扫描目录失败", e);
            return Result.error("添加失败: " + e.getMessage());
        }
    }

    /**
     * 更新扫描目录配置
     */
    @PutMapping("/{id}")
    public Result<ScanDirectoryDTO> updateDirectory(
            @PathVariable String id,
            @RequestBody ScanDirectoryDTO directory) {
        try {
            log.info("更新扫描目录: {}", id);
            ScanDirectoryDTO result = scanDirectoryService.updateDirectory(id, directory);
            return Result.success("更新成功", result);
        } catch (Exception e) {
            log.error("更新扫描目录失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除扫描目录配置
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteDirectory(@PathVariable String id) {
        try {
            log.info("删除扫描目录: {}", id);
            scanDirectoryService.deleteDirectory(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除扫描目录失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 记录使用次数
     */
    @PostMapping("/{id}/usage")
    public Result<String> recordUsage(@PathVariable String id) {
        try {
            scanDirectoryService.recordUsage(id);
            return Result.success("记录成功");
        } catch (Exception e) {
            log.error("记录使用失败", e);
            return Result.error("记录失败: " + e.getMessage());
        }
    }

    /**
     * 批量导入配置
     */
    @PostMapping("/import")
    public Result<List<ScanDirectoryDTO>> importDirectories(@RequestBody List<ScanDirectoryDTO> directories) {
        try {
            log.info("导入扫描目录: {} 个", directories.size());
            List<ScanDirectoryDTO> result = scanDirectoryService.importDirectories(directories);
            return Result.success("导入成功", result);
        } catch (Exception e) {
            log.error("导入扫描目录失败", e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }
}
