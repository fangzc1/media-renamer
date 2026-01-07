package com.mediarenamer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频文件信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFileDTO {

    /**
     * 唯一标识符 (UUID)
     * 用于前端状态管理 (选中、展开等)
     */
    private String id;

    /**
     * 文件完整路径
     */
    private String filePath;

    /**
     * 文件名 (不含扩展名)
     */
    private String fileName;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * 文件大小 (字节)
     */
    private Long fileSize;

    /**
     * 文件大小 (可读格式, 如: 1.2 GB)
     */
    private String fileSizeReadable;

    /**
     * 媒体类型: MOVIE(电影) / TV_SHOW(电视剧) / UNKNOWN(未知)
     */
    private String mediaType;

    /**
     * 解析出的标题
     */
    private String parsedTitle;

    /**
     * 解析出的年份
     */
    private Integer parsedYear;

    /**
     * 解析出的季数 (电视剧)
     */
    private Integer parsedSeason;

    /**
     * 解析出的集数 (电视剧)
     */
    private Integer parsedEpisode;

    /**
     * 扫描根目录路径 (用于重命名时构建目标路径)
     */
    private String scanRootPath;

    /**
     * 父目录名称 (例如: 微观小世界.第一碟全.2006...)
     * 用于在文件名匹配失败时提供备选搜索关键词
     */
    private String parentDirectory;

    /**
     * 祖父目录名称 (例如: 微观小世界)
     * 用于处理 "ShowName/Season 1/file.mkv" 或 "ShowName/Disc 1/file.mkv" 结构
     * 通常是剧集的根目录,包含核心剧名信息
     */
    private String grandParentDirectory;

    /**
     * 匹配的媒体信息 (电影或电视剧)
     * 使用 Object 类型以兼容 TmdbMovieDTO 和 TmdbTvShowDTO
     */
    private Object matchedInfo;

}
