package com.mediarenamer.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 季目录特征匹配工具
 * 用于识别标准的季文件夹命名格式，支持智能目录回溯
 *
 * 支持的格式:
 * - Season 01, Season 1 (标准英文)
 * - S1, S02 (缩写英文)
 * - 第1季, 第一季 (中文格式)
 * - Specials, Special (特别篇)
 */
@Slf4j
public class SeasonDirectoryMatcher {

    // 季目录匹配模式

    /** 标准英文格式: Season 01, Season 1 */
    private static final Pattern SEASON_STANDARD = Pattern.compile(
            "^Season\\s*(\\d+)$",
            Pattern.CASE_INSENSITIVE
    );

    /** 缩写英文格式: S1, S02 */
    private static final Pattern SEASON_SHORT = Pattern.compile(
            "^S(\\d+)$",
            Pattern.CASE_INSENSITIVE
    );

    /** 中文格式: 第1季, 第一季 */
    private static final Pattern SEASON_CHINESE = Pattern.compile(
            "^第\\s*(\\d+|[一二三四五六七八九十]+)\\s*季$"
    );

    /** 特别篇格式: Specials, Special */
    private static final Pattern SEASON_SPECIALS = Pattern.compile(
            "^Specials?$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 季信息结构
     */
    @Getter
    public static class SeasonInfo {
        private final int seasonNumber;
        private final String originalName;
        private final String matchType;

        public SeasonInfo(int seasonNumber, String originalName, String matchType) {
            this.seasonNumber = seasonNumber;
            this.originalName = originalName;
            this.matchType = matchType;
        }

        @Override
        public String toString() {
            return String.format("Season %d (%s, type=%s)", seasonNumber, originalName, matchType);
        }
    }

    /**
     * 解析目录名，判断是否为季目录
     *
     * @param directoryName 目录名称
     * @return 季信息，如果不是季目录则返回 null
     */
    public static SeasonInfo parseSeasonFolder(String directoryName) {
        if (directoryName == null || directoryName.trim().isEmpty()) {
            return null;
        }

        String trimmed = directoryName.trim();

        // 1. 尝试匹配标准英文格式
        Matcher standardMatcher = SEASON_STANDARD.matcher(trimmed);
        if (standardMatcher.matches()) {
            int season = Integer.parseInt(standardMatcher.group(1));
            log.debug("匹配到标准季目录: {} -> Season {}", directoryName, season);
            return new SeasonInfo(season, directoryName, "STANDARD_EN");
        }

        // 2. 尝试匹配缩写英文格式
        Matcher shortMatcher = SEASON_SHORT.matcher(trimmed);
        if (shortMatcher.matches()) {
            int season = Integer.parseInt(shortMatcher.group(1));
            log.debug("匹配到缩写季目录: {} -> Season {}", directoryName, season);
            return new SeasonInfo(season, directoryName, "SHORT_EN");
        }

        // 3. 尝试匹配特别篇格式
        Matcher specialsMatcher = SEASON_SPECIALS.matcher(trimmed);
        if (specialsMatcher.matches()) {
            log.debug("匹配到特别篇目录: {} -> Season 0", directoryName);
            return new SeasonInfo(0, directoryName, "SPECIALS");
        }

        // 4. 尝试匹配中文格式
        Matcher chineseMatcher = SEASON_CHINESE.matcher(trimmed);
        if (chineseMatcher.matches()) {
            String seasonStr = chineseMatcher.group(1);
            int season = parseChineseNumber(seasonStr);
            if (season > 0) {
                log.debug("匹配到中文季目录: {} -> Season {}", directoryName, season);
                return new SeasonInfo(season, directoryName, "CHINESE");
            }
        }

        // 不匹配任何季目录模式
        return null;
    }

    /**
     * 转换中文数字为阿拉伯数字
     * 支持: 一二三四五六七八九十 + 阿拉伯数字
     */
    private static int parseChineseNumber(String chineseNum) {
        if (chineseNum == null || chineseNum.isEmpty()) {
            return -1;
        }

        // 如果已经是阿拉伯数字，直接解析
        try {
            return Integer.parseInt(chineseNum);
        } catch (NumberFormatException e) {
            // 继续处理中文数字
        }

        // 简单的中文数字转换 (支持 1-20)
        String num = chineseNum.trim();
        switch (num) {
            case "一": return 1;
            case "二": return 2;
            case "三": return 3;
            case "四": return 4;
            case "五": return 5;
            case "六": return 6;
            case "七": return 7;
            case "八": return 8;
            case "九": return 9;
            case "十": return 10;
            case "十一": return 11;
            case "十二": return 12;
            case "十三": return 13;
            case "十四": return 14;
            case "十五": return 15;
            case "十六": return 16;
            case "十七": return 17;
            case "十八": return 18;
            case "十九": return 19;
            case "二十": return 20;
            default:
                log.warn("无法解析中文数字: {}", chineseNum);
                return -1;
        }
    }

    /**
     * 检测目录名是否为季目录
     *
     * @param directoryName 目录名称
     * @return true 如果是季目录
     */
    public static boolean isSeasonFolder(String directoryName) {
        return parseSeasonFolder(directoryName) != null;
    }
}
