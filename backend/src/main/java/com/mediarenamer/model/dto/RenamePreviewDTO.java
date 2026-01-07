package com.mediarenamer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 重命名预览 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenamePreviewDTO {

    /**
     * 原文件路径
     */
    private String oldPath;

    /**
     * 新文件路径
     */
    private String newPath;

    /**
     * 原文件名
     */
    private String oldFileName;

    /**
     * 新文件名
     */
    private String newFileName;

    /**
     * 原相对目录路径（相对于扫描根目录，如 "./" 或 "./subdir/"）
     */
    private String oldRelativeDirectory;

    /**
     * 纯原文件名（不含路径，如 "不眠日 - S01E01.mp4"）
     */
    private String pureOldFileName;

    /**
     * 新相对目录路径（相对于扫描根目录，如 "./不眠日 (2020)/Season 1/"）
     */
    private String newRelativeDirectory;

    /**
     * 纯新文件名（不含路径，如 "不眠日-S01E01.mp4"）
     */
    private String pureNewFileName;

    /**
     * 是否需要创建新目录
     */
    private Boolean needCreateDirectory;

    /**
     * 状态: pending(待处理) / success(成功) / failed(失败)
     */
    private String status;

    /**
     * 错误信息 (如果失败)
     */
    private String errorMessage;

    /**
     * 额外的元数据信息，用于前端分组和统计
     * 包含的键值:
     * - mediaType: String (MOVIE / TV)
     * - groupKey: String (分组的唯一标识)
     * - seriesName: String (剧集名称)
     * - seasonNumber: Integer (季号)
     * - episodeNumber: Integer (集号)
     * - seasonTotalEpisodes: Integer (该季度的总集数)
     */
    private Map<String, Object> metadata;

}
