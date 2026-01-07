package com.mediarenamer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 扫描目录配置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanDirectoryDTO {

    /**
     * 配置 ID
     */
    private String id;

    /**
     * 目录路径
     */
    private String path;

    /**
     * 目录名称（用户自定义）
     */
    private String name;

    /**
     * 媒体类型: MOVIE(电影) / TV_SHOW(电视剧) / MIXED(混合)
     */
    private String mediaType;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 备注说明
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;

    /**
     * 使用次数
     */
    private Integer usageCount;
}
