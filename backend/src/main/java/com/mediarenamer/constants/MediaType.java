package com.mediarenamer.constants;

/**
 * 媒体类型常量
 *
 * 统一管理媒体类型字符串,避免硬编码
 */
public final class MediaType {

    /**
     * 电影
     */
    public static final String MOVIE = "MOVIE";

    /**
     * 电视剧
     */
    public static final String TV_SHOW = "TV_SHOW";

    /**
     * 未知类型
     */
    public static final String UNKNOWN = "UNKNOWN";

    /**
     * 混合类型 (自动识别)
     */
    public static final String MIXED = "MIXED";

    private MediaType() {
        // 工具类不允许实例化
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
