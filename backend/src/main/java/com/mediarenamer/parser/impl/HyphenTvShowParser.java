package com.mediarenamer.parser.impl;

import com.mediarenamer.parser.MediaParser;
import com.mediarenamer.parser.ParseResult;
import com.mediarenamer.parser.ParsingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 连字符格式电视剧解析器 (1x01 格式)
 *
 * 支持格式:
 * - Breaking.Bad.1x01.mkv
 * - Show.10x05.mkv
 */
@Slf4j
@Component
public class HyphenTvShowParser implements MediaParser {

    // 1x01 格式
    private static final Pattern TV_HYPHEN = Pattern.compile(
            "(.+?)[.\\s_-]+(\\d{1,2})x(\\d{1,3}).*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ParseResult tryParse(ParsingContext context) {
        String fileName = context.getFileNameWithoutExt();
        Matcher matcher = TV_HYPHEN.matcher(fileName);

        if (matcher.matches()) {
            String title = matcher.group(1)
                    .replaceAll("[.\\s_-]+", " ")
                    .trim();

            int season = Integer.parseInt(matcher.group(2));
            int episode = Integer.parseInt(matcher.group(3));

            ParseResult result = ParseResult.builder()
                    .successful(true)
                    .mediaType("TV_SHOW")
                    .title(title)
                    .season(season)
                    .episode(episode)
                    .confidence(0.9)
                    .parserName(getName())
                    .build();

            log.debug("电视剧解析成功 (1x01): {} -> 剧名={}, {}x{}",
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
        return 85; // 高优先级
    }

    @Override
    public boolean canParse(ParsingContext context) {
        // 快速预检查: 文件名包含 #x# 模式
        String fileName = context.getFileNameWithoutExt().toLowerCase();
        return fileName.matches(".*\\d{1,2}x\\d{1,3}.*");
    }
}
