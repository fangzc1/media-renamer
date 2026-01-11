package com.mediarenamer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mediarenamer.model.dto.ScanDirectoryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 扫描目录配置服务
 * 管理用户的扫描目录预设，支持持久化存储
 */
@Slf4j
@Service
public class ScanDirectoryService {

    @Value("${config.scan-directories-file}")
    private String configFilePath;

    private final ObjectMapper objectMapper;

    public ScanDirectoryService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 初始化后确保配置目录存在
     */
    @PostConstruct
    public void init() {
        ensureConfigDirExists();
    }

    /**
     * 确保配置目录存在
     */
    private void ensureConfigDirExists() {
        File configFile = new File(configFilePath);
        File configDir = configFile.getParentFile();
        if (!configDir.exists()) {
            boolean created = configDir.mkdirs();
            if (created) {
                log.info("创建配置目录: {}", configDir.getAbsolutePath());
            }
        }
    }

    /**
     * 获取所有扫描目录配置
     */
    public List<ScanDirectoryDTO> getAllDirectories() {
        try {
            File file = new File(configFilePath);
            if (!file.exists()) {
                return new ArrayList<>();
            }

            List<ScanDirectoryDTO> directories = objectMapper.readValue(
                    file,
                    new TypeReference<List<ScanDirectoryDTO>>() {}
            );

            log.info("加载扫描目录配置: {} 个", directories.size());
            return directories;
        } catch (IOException e) {
            log.error("读取扫描目录配置失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取启用的扫描目录
     */
    public List<ScanDirectoryDTO> getEnabledDirectories() {
        return getAllDirectories().stream()
                .filter(ScanDirectoryDTO::getEnabled)
                .collect(Collectors.toList());
    }

    /**
     * 添加扫描目录配置
     */
    public ScanDirectoryDTO addDirectory(ScanDirectoryDTO directory) {
        List<ScanDirectoryDTO> directories = getAllDirectories();

        // 生成 ID
        if (directory.getId() == null) {
            directory.setId(UUID.randomUUID().toString());
        }

        // 设置默认值
        if (directory.getEnabled() == null) {
            directory.setEnabled(true);
        }
        if (directory.getCreatedAt() == null) {
            directory.setCreatedAt(LocalDateTime.now());
        }
        if (directory.getUsageCount() == null) {
            directory.setUsageCount(0);
        }

        directories.add(directory);
        saveDirectories(directories);

        log.info("添加扫描目录: {} -> {}", directory.getName(), directory.getPath());
        return directory;
    }

    /**
     * 更新扫描目录配置
     */
    public ScanDirectoryDTO updateDirectory(String id, ScanDirectoryDTO updates) {
        List<ScanDirectoryDTO> directories = getAllDirectories();

        for (int i = 0; i < directories.size(); i++) {
            if (directories.get(i).getId().equals(id)) {
                ScanDirectoryDTO existing = directories.get(i);

                // 更新字段
                if (updates.getName() != null) {
                    existing.setName(updates.getName());
                }
                if (updates.getPath() != null) {
                    existing.setPath(updates.getPath());
                }
                if (updates.getMediaType() != null) {
                    existing.setMediaType(updates.getMediaType());
                }
                if (updates.getEnabled() != null) {
                    existing.setEnabled(updates.getEnabled());
                }
                if (updates.getDescription() != null) {
                    existing.setDescription(updates.getDescription());
                }

                directories.set(i, existing);
                saveDirectories(directories);

                log.info("更新扫描目录: {}", id);
                return existing;
            }
        }

        throw new RuntimeException("扫描目录不存在: " + id);
    }

    /**
     * 删除扫描目录配置
     */
    public void deleteDirectory(String id) {
        List<ScanDirectoryDTO> directories = getAllDirectories();
        boolean removed = directories.removeIf(d -> d.getId().equals(id));

        if (removed) {
            saveDirectories(directories);
            log.info("删除扫描目录: {}", id);
        } else {
            throw new RuntimeException("扫描目录不存在: " + id);
        }
    }

    /**
     * 记录使用次数
     */
    public void recordUsage(String id) {
        List<ScanDirectoryDTO> directories = getAllDirectories();

        for (ScanDirectoryDTO directory : directories) {
            if (directory.getId().equals(id)) {
                directory.setLastUsedAt(LocalDateTime.now());
                directory.setUsageCount(directory.getUsageCount() + 1);
                saveDirectories(directories);
                log.debug("记录使用: {} (第{}次)", directory.getName(), directory.getUsageCount());
                break;
            }
        }
    }

    /**
     * 保存配置到文件
     */
    private void saveDirectories(List<ScanDirectoryDTO> directories) {
        try {
            File file = new File(configFilePath);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, directories);
            log.debug("保存扫描目录配置: {} 个", directories.size());
        } catch (IOException e) {
            log.error("保存扫描目录配置失败", e);
            throw new RuntimeException("保存配置失败", e);
        }
    }

    /**
     * 导入配置（批量添加）
     */
    public List<ScanDirectoryDTO> importDirectories(List<ScanDirectoryDTO> newDirectories) {
        List<ScanDirectoryDTO> existing = getAllDirectories();

        for (ScanDirectoryDTO directory : newDirectories) {
            if (directory.getId() == null) {
                directory.setId(UUID.randomUUID().toString());
            }
            if (directory.getEnabled() == null) {
                directory.setEnabled(true);
            }
            if (directory.getCreatedAt() == null) {
                directory.setCreatedAt(LocalDateTime.now());
            }
            if (directory.getUsageCount() == null) {
                directory.setUsageCount(0);
            }
            existing.add(directory);
        }

        saveDirectories(existing);
        log.info("导入扫描目录: {} 个", newDirectories.size());

        return existing;
    }
}
