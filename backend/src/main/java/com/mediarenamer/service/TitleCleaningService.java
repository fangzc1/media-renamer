package com.mediarenamer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 标题清洗服务
 *
 * 负责清洗解析出的标题,移除干扰信息:
 * 1. 尾部索引 ([1], (2), 【3】)
 * 2. 提取书名号内容 (《...》)
 * 3. 首部标签 ([4K], 【合集】)
 * 4. 技术标签 (1080p, BluRay, 中字等)
 */
@Slf4j
@Service
public class TitleCleaningService {

    // === 原有清洗规则 ===

    // 尾部索引: [1], (2), 【3】
    private static final Pattern TAIL_INDEX_PATTERN = Pattern.compile(
            "[\\[\\(【]\\s*\\d+\\s*[\\]\\)】]\\s*$"
    );

    // 书名号内容: 《...》
    private static final Pattern BOOK_TITLE_PATTERN = Pattern.compile(
            "《(.+?)》"
    );

    // 首部标签: [...] 或 【...】
    private static final Pattern HEAD_TAG_PATTERN = Pattern.compile(
            "^[\\[【][^\\]】]+[\\]】]\\s*"
    );

    // === 新增技术标签清洗规则 (使用单词边界匹配,避免误清洗) ===

    // 分辨率标签: 1080p, 720p, 4K, UHD等
    private static final Pattern RESOLUTION_TAG = Pattern.compile(
            "\\b(1080p|720p|2160p|4K|UHD|HD|SD|FHD|QHD)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    // 视频编码标签: x264, x265, HEVC, H264等
    private static final Pattern CODEC_TAG = Pattern.compile(
            "\\b(x264|x265|H264|H265|HEVC|AVC|VP9|AV1)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    // 音频编码标签: AAC, AC3, DTS等
    private static final Pattern AUDIO_TAG = Pattern.compile(
            "\\b(AAC|AC3|DTS|DD5\\.1|DD2\\.0|Atmos|TrueHD)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    // 来源标签: BluRay, WEB-DL, HDRip, DVDRip等
    private static final Pattern SOURCE_TAG = Pattern.compile(
            "\\b(BluRay|BRRip|WEB-DL|WEBRip|HDRip|DVDRip|HDTV|BDRip|" +
            "Remux|REPACK|PROPER)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    // 字幕标签: 中字, 中英, 双语, 简繁等
    private static final Pattern SUBTITLE_TAG = Pattern.compile(
            "\\b(中字|中英|双语|简繁|内封|外挂|SUB|字幕组|内嵌|繁体|简体|" +
            "英字|日字|韩字|CHT|CHS)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    // 发布组标签: [RARBG], -YIFY等
    private static final Pattern RELEASE_GROUP_TAG = Pattern.compile(
            "[.\\s_-]*[-\\[\\(【](RARBG|YIFY|YTS|ETRG|PublicHD|FGT|EVO|" +
            "ION10|SPARKS|AMZN|NF|HBO)[\\]\\)】].*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 清洗标题 (完整流程)
     *
     * @param title 原始标题
     * @return 清洗后的标题
     */
    public String cleanTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return title;
        }

        String cleaned = title;

        // 步骤1: 移除尾部索引
        cleaned = removeTrailingIndex(cleaned);

        // 步骤2: 智能提取书名号内容
        cleaned = extractBookTitle(cleaned);

        // 步骤3: 移除首部标签
        cleaned = removeLeadingTags(cleaned);

        // 步骤4: 移除技术标签 (新增)
        cleaned = removeTechnicalTags(cleaned);

        // 如果清洗后为空,返回原标题
        if (cleaned.trim().isEmpty()) {
            log.warn("标题清洗后为空,返回原标题: {}", title);
            return title;
        }

        // 记录清洗结果
        if (!cleaned.equals(title)) {
            log.debug("标题清洗: {} -> {}", title, cleaned);
        }

        return cleaned;
    }

    /**
     * 移除尾部索引
     * 示例: "剧名[1]" -> "剧名"
     */
    public String removeTrailingIndex(String title) {
        return TAIL_INDEX_PATTERN.matcher(title).replaceAll("").trim();
    }

    /**
     * 提取书名号内容
     * 示例: "[4K]《Tiny World》" -> "Tiny World"
     */
    public String extractBookTitle(String title) {
        // 如果标题以标签开头且包含书名号,优先提取书名号内容
        if ((title.startsWith("[") || title.startsWith("【")) && title.contains("《")) {
            var bookTitleMatcher = BOOK_TITLE_PATTERN.matcher(title);
            if (bookTitleMatcher.find()) {
                String bookTitle = bookTitleMatcher.group(1).trim();
                if (!bookTitle.isEmpty()) {
                    log.debug("提取书名号内容: {} -> {}", title, bookTitle);
                    return bookTitle;
                }
            }
        }
        return title;
    }

    /**
     * 移除首部标签
     * 示例: "[4K]剧名" -> "剧名"
     */
    public String removeLeadingTags(String title) {
        String cleaned = title;

        // 循环移除,直到没有首部标签
        int maxIterations = 10; // 防止无限循环
        int iterations = 0;

        while (iterations < maxIterations) {
            String before = cleaned;
            cleaned = HEAD_TAG_PATTERN.matcher(cleaned).replaceFirst("").trim();

            if (cleaned.equals(before)) {
                break; // 没有更多标签可移除
            }
            iterations++;
        }

        return cleaned;
    }

    /**
     * 移除技术标签 (新增功能)
     * 移除分辨率、编码、字幕等技术信息
     */
    public String removeTechnicalTags(String title) {
        String cleaned = title;

        // 按顺序移除各类技术标签
        cleaned = RESOLUTION_TAG.matcher(cleaned).replaceAll("").trim();
        cleaned = SOURCE_TAG.matcher(cleaned).replaceAll("").trim();
        cleaned = CODEC_TAG.matcher(cleaned).replaceAll("").trim();
        cleaned = AUDIO_TAG.matcher(cleaned).replaceAll("").trim();
        cleaned = SUBTITLE_TAG.matcher(cleaned).replaceAll("").trim();
        cleaned = RELEASE_GROUP_TAG.matcher(cleaned).replaceAll("").trim();

        return cleaned;
    }

    /**
     * 仅移除技术标签 (不执行其他清洗)
     * 用于需要保留标签但移除技术信息的场景
     */
    public String removeTechnicalTagsOnly(String title) {
        if (title == null || title.trim().isEmpty()) {
            return title;
        }
        return removeTechnicalTags(title);
    }
}
