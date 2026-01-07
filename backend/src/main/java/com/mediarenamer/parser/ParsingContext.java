package com.mediarenamer.parser;

import lombok.Builder;
import lombok.Data;

/**
 * 解析上下文
 *
 * 包含文件名解析所需的所有上下文信息
 */
@Data
@Builder
public class ParsingContext {

    /**
     * 原始文件名 (包含扩展名)
     */
    private String fileName;

    /**
     * 文件名 (不含扩展名)
     */
    private String fileNameWithoutExt;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * 父目录名称
     */
    private String parentDirectory;

    /**
     * 祖父目录名称
     */
    private String grandParentDirectory;

    /**
     * 用户预设的媒体类型 (MOVIE/TV_SHOW/MIXED/null)
     */
    private String preferredMediaType;

    /**
     * 扫描根目录路径
     */
    private String scanRootPath;

    /**
     * 父目录是否为季目录
     */
    @Builder.Default
    private boolean parentIsSeasonFolder = false;

    /**
     * 推断的剧集名称 (从目录结构获取)
     */
    private String detectedSeriesName;

    /**
     * 判断是否有用户预设类型
     *
     * @return true 如果用户指定了媒体类型
     */
    public boolean hasPreferredType() {
        return preferredMediaType != null &&
               !preferredMediaType.isEmpty() &&
               !"MIXED".equalsIgnoreCase(preferredMediaType);
    }

    /**
     * 判断是否为指定的预设类型
     *
     * @param mediaType 媒体类型
     * @return true 如果匹配预设类型
     */
    public boolean isPreferredType(String mediaType) {
        return hasPreferredType() &&
               preferredMediaType.equalsIgnoreCase(mediaType);
    }

    /**
     * 判断是否为强制电影模式
     *
     * @return true 如果用户强制指定为电影
     */
    public boolean isForceMovieMode() {
        return "MOVIE".equalsIgnoreCase(preferredMediaType);
    }

    /**
     * 判断是否为强制电视剧模式
     *
     * @return true 如果用户强制指定为电视剧
     */
    public boolean isForceTvShowMode() {
        return "TV_SHOW".equalsIgnoreCase(preferredMediaType);
    }

    /**
     * 判断是否为自动模式
     *
     * @return true 如果未指定或指定为 MIXED
     */
    public boolean isAutoMode() {
        return preferredMediaType == null ||
               preferredMediaType.isEmpty() ||
               "MIXED".equalsIgnoreCase(preferredMediaType);
    }
}
