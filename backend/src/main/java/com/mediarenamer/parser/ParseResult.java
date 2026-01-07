package com.mediarenamer.parser;

import lombok.Builder;
import lombok.Data;

/**
 * 解析结果
 *
 * 包含解析出的所有媒体信息和置信度评分
 */
@Data
@Builder
public class ParseResult {

    /**
     * 媒体类型: MOVIE / TV_SHOW / UNKNOWN
     */
    private String mediaType;

    /**
     * 解析出的标题
     */
    private String title;

    /**
     * 解析出的年份 (可选)
     */
    private Integer year;

    /**
     * 解析出的季数 (电视剧)
     */
    private Integer season;

    /**
     * 解析出的集数 (电视剧)
     */
    private Integer episode;

    /**
     * 解析置信度 (0.0-1.0)
     * 用于在多个解析器都匹配时选择最佳结果
     */
    @Builder.Default
    private Double confidence = 1.0;

    /**
     * 解析器名称
     * 用于调试和日志
     */
    private String parserName;

    /**
     * 是否成功解析
     */
    @Builder.Default
    private boolean successful = false;

    /**
     * 创建失败结果
     *
     * @return 失败的解析结果
     */
    public static ParseResult failure() {
        return ParseResult.builder()
                .successful(false)
                .confidence(0.0)
                .build();
    }

    /**
     * 创建未知类型结果
     *
     * @param title 使用文件名作为标题
     * @return 未知类型的解析结果
     */
    public static ParseResult unknown(String title) {
        return ParseResult.builder()
                .successful(true)
                .mediaType("UNKNOWN")
                .title(title)
                .confidence(0.1)
                .parserName("FallbackParser")
                .build();
    }
}
