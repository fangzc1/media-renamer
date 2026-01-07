package com.mediarenamer.parser.impl;

import com.mediarenamer.parser.MediaParser;
import com.mediarenamer.parser.ParseResult;
import com.mediarenamer.parser.ParsingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 灵活的电影解析器
 *
 * 支持多种电影文件名格式:
 * 1. 标准格式 (带年份): Movie.Title.2020.1080p.mkv
 * 2. 括号年份格式: Movie.Title.(2020).mkv
 * 3. 无年份格式: Movie.Title.1080p.BluRay.mkv
 */
@Slf4j
@Component
public class FlexibleMovieParser implements MediaParser {

    // 标准电影格式: 标题 + 年份 + 其他信息
    private static final Pattern MOVIE_WITH_YEAR = Pattern.compile(
            "(.+?)[.\\s_-]+(\\d{4})[.\\s_-]+.*",
            Pattern.CASE_INSENSITIVE
    );

    // 括号年份格式: 标题 + (年份)
    private static final Pattern MOVIE_BRACKET_YEAR = Pattern.compile(
            "(.+?)[.\\s_-]*\\((\\d{4})\\).*",
            Pattern.CASE_INSENSITIVE
    );

    // 无年份电影格式: 标题 + 技术特征 (分辨率/编码)
    private static final Pattern MOVIE_NO_YEAR = Pattern.compile(
            "(.+?)[.\\s_-]+(1080p|720p|2160p|4K|UHD|BluRay|WEB-DL|HDRip|BRRip|DVDRip).*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ParseResult tryParse(ParsingContext context) {
        String fileName = context.getFileNameWithoutExt();

        // 优先级1: 尝试括号年份格式
        ParseResult bracketResult = tryParseBracketYear(fileName);
        if (bracketResult.isSuccessful()) {
            bracketResult.setParserName(getName());
            log.debug("电影解析成功 (括号年份): {} -> 标题={}, 年份={}",
                    fileName, bracketResult.getTitle(), bracketResult.getYear());
            return bracketResult;
        }

        // 优先级2: 尝试标准年份格式
        ParseResult standardResult = tryParseStandardYear(fileName);
        if (standardResult.isSuccessful()) {
            standardResult.setParserName(getName());
            log.debug("电影解析成功 (标准格式): {} -> 标题={}, 年份={}",
                    fileName, standardResult.getTitle(), standardResult.getYear());
            return standardResult;
        }

        // 优先级3: 尝试无年份格式 (仅在强制电影模式下)
        if (context.isForceMovieMode()) {
            ParseResult noYearResult = tryParseNoYear(fileName);
            if (noYearResult.isSuccessful()) {
                noYearResult.setParserName(getName());
                log.debug("电影解析成功 (无年份): {} -> 标题={}",
                        fileName, noYearResult.getTitle());
                return noYearResult;
            }

            // 兜底: 使用文件名作为标题
            log.debug("电影解析失败,使用文件名作为标题: {}", fileName);
            return ParseResult.builder()
                    .successful(true)
                    .mediaType("MOVIE")
                    .title(fileName)
                    .confidence(0.3)
                    .parserName(getName())
                    .build();
        }

        return ParseResult.failure();
    }

    /**
     * 解析括号年份格式
     * 示例: The.Dark.Knight.(2008).mkv -> The Dark Knight, 2008
     */
    private ParseResult tryParseBracketYear(String fileName) {
        Matcher matcher = MOVIE_BRACKET_YEAR.matcher(fileName);
        if (matcher.matches()) {
            String title = matcher.group(1)
                    .replaceAll("[.\\s_-]+", " ")
                    .trim();
            int year = Integer.parseInt(matcher.group(2));

            return ParseResult.builder()
                    .successful(true)
                    .mediaType("MOVIE")
                    .title(title)
                    .year(year)
                    .confidence(0.95)
                    .build();
        }
        return ParseResult.failure();
    }

    /**
     * 解析标准年份格式
     * 示例: Inception.2010.1080p.BluRay.mkv -> Inception, 2010
     */
    private ParseResult tryParseStandardYear(String fileName) {
        Matcher matcher = MOVIE_WITH_YEAR.matcher(fileName);
        if (matcher.matches()) {
            String title = matcher.group(1)
                    .replaceAll("[.\\s_-]+", " ")
                    .trim();
            int year = Integer.parseInt(matcher.group(2));

            // 验证年份合理性 (1900-2099)
            if (year < 1900 || year > 2099) {
                return ParseResult.failure();
            }

            return ParseResult.builder()
                    .successful(true)
                    .mediaType("MOVIE")
                    .title(title)
                    .year(year)
                    .confidence(0.9)
                    .build();
        }
        return ParseResult.failure();
    }

    /**
     * 解析无年份格式 (通过技术特征识别)
     * 示例: Inception.1080p.BluRay.mkv -> Inception
     */
    private ParseResult tryParseNoYear(String fileName) {
        Matcher matcher = MOVIE_NO_YEAR.matcher(fileName);
        if (matcher.matches()) {
            String title = matcher.group(1)
                    .replaceAll("[.\\s_-]+", " ")
                    .trim();

            return ParseResult.builder()
                    .successful(true)
                    .mediaType("MOVIE")
                    .title(title)
                    .confidence(0.7) // 置信度较低
                    .build();
        }
        return ParseResult.failure();
    }

    @Override
    public String getSupportedType() {
        return "MOVIE";
    }

    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }

    @Override
    public boolean canParse(ParsingContext context) {
        String fileName = context.getFileNameWithoutExt().toLowerCase();

        // 排除明显的电视剧格式 (避免假阳性)
        if (fileName.matches(".*s\\d{1,2}e\\d{1,3}.*") ||
            fileName.matches(".*\\d{1,2}x\\d{1,3}.*") ||
            fileName.matches(".*\\be\\d{1,3}\\b.*")) {
            return false;
        }

        // 检查电影特征: 年份或技术标签
        return fileName.matches(".*\\d{4}.*") ||
               fileName.matches(".*(1080p|720p|4K|BluRay|WEB-DL|HDRip).*");
    }
}
