package com.mediarenamer.parser.impl;

import com.mediarenamer.parser.MediaParser;
import com.mediarenamer.parser.ParseResult;
import com.mediarenamer.parser.ParsingContext;
import com.mediarenamer.util.SeasonDirectoryMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 标准电视剧解析器 (SxxExx 格式)
 *
 * 支持格式:
 * - Breaking.Bad.S01E01.mkv
 * - Minuscule.2006.S01E01.mkv (带年份)
 */
@Slf4j
@Component
public class StandardTvShowParser implements MediaParser {

    // 标准 SxxExx 格式 (支持可选年份)
    private static final Pattern TV_SXXEXX = Pattern.compile(
            "(.+?)(?:[.\\s_-]+(\\d{4}))?[.\\s_-]+[Ss](\\d{1,2})[Ee](\\d{1,3}).*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ParseResult tryParse(ParsingContext context) {
        String fileName = context.getFileNameWithoutExt();
        Matcher matcher = TV_SXXEXX.matcher(fileName);

        if (matcher.matches()) {
            String title = matcher.group(1)
                    .replaceAll("[.\\s_-]+", " ")
                    .trim();

            // 提取可选年份
            String yearStr = matcher.group(2);
            Integer year = null;
            if (yearStr != null && !yearStr.isEmpty()) {
                try {
                    year = Integer.parseInt(yearStr);
                } catch (NumberFormatException e) {
                    log.warn("解析年份失败: {}", yearStr);
                }
            }

            int season = Integer.parseInt(matcher.group(3));
            int episode = Integer.parseInt(matcher.group(4));

            ParseResult result = ParseResult.builder()
                    .successful(true)
                    .mediaType("TV_SHOW")
                    .title(title)
                    .year(year)
                    .season(season)
                    .episode(episode)
                    .confidence(0.95) // SxxExx 格式置信度最高
                    .parserName(getName())
                    .build();

            log.debug("电视剧解析成功 (SxxExx): {} -> 剧名={}, S{}E{}",
                    fileName, title, season, episode);

            return result;
        }

        return ParseResult.failure();
    }

    @Override
    public String getSupportedType() {
        return "TV_SHOW";
    }

    @Override
    public int getPriority() {
        return 90; // 最高优先级 (SxxExx 是最标准的格式)
    }

    @Override
    public boolean canParse(ParsingContext context) {
        // 快速预检查: 文件名包含 SxxExx 模式
        String fileName = context.getFileNameWithoutExt().toLowerCase();
        return fileName.matches(".*s\\d{1,2}e\\d{1,3}.*");
    }
}
