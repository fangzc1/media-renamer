package com.mediarenamer.parser.impl;

import com.mediarenamer.parser.MediaParser;
import com.mediarenamer.parser.ParseResult;
import com.mediarenamer.parser.ParsingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 括号格式电视剧解析器
 *
 * 支持格式:
 * - Show [S01E01].mkv
 * - Show.[1x01].mkv
 */
@Slf4j
@Component
public class BracketTvShowParser implements MediaParser {

    // 括号 SxxExx 格式
    private static final Pattern TV_BRACKET_SXXEXX = Pattern.compile(
            "(.+?)[.\\s_-]*\\[S(\\d{1,2})E(\\d{1,3})\\].*",
            Pattern.CASE_INSENSITIVE
    );

    // 括号 1x01 格式
    private static final Pattern TV_BRACKET_HYPHEN = Pattern.compile(
            "(.+?)[.\\s_-]*\\[(\\d{1,2})x(\\d{1,3})\\].*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ParseResult tryParse(ParsingContext context) {
        String fileName = context.getFileNameWithoutExt();

        // 尝试括号 SxxExx 格式
        Matcher sxxexxMatcher = TV_BRACKET_SXXEXX.matcher(fileName);
        if (sxxexxMatcher.matches()) {
            return buildResult(sxxexxMatcher, fileName, "SxxExx");
        }

        // 尝试括号 1x01 格式
        Matcher hyphenMatcher = TV_BRACKET_HYPHEN.matcher(fileName);
        if (hyphenMatcher.matches()) {
            return buildResult(hyphenMatcher, fileName, "1x01");
        }

        return ParseResult.failure();
    }

    private ParseResult buildResult(Matcher matcher, String fileName, String format) {
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
                .confidence(0.88)
                .parserName(getName())
                .build();

        log.debug("电视剧解析成功 (括号{}): {} -> 剧名={}, S{}E{}",
                format, fileName, title, season, episode);

        return result;
    }

    @Override
    public String getSupportedType() {
        return "TV_SHOW";
    }

    @Override
    public int getPriority() {
        return 82; // 高优先级
    }

    @Override
    public boolean canParse(ParsingContext context) {
        // 快速预检查: 文件名包含括号内的集数信息
        String fileName = context.getFileNameWithoutExt().toLowerCase();
        return fileName.matches(".*\\[.*[se]\\d+.*\\].*") ||
               fileName.matches(".*\\[\\d+x\\d+\\].*");
    }
}
