package com.mediarenamer.parser.impl;

import com.mediarenamer.parser.MediaParser;
import com.mediarenamer.parser.ParseResult;
import com.mediarenamer.parser.ParsingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Season.Episode 完整单词格式解析器
 *
 * 支持格式:
 * - Show.Season.1.Episode.5.mkv
 * - Show.season1.episode5.mkv
 */
@Slf4j
@Component
public class SeasonEpisodeTvShowParser implements MediaParser {

    // season.episode 格式
    private static final Pattern TV_SEASON_EPISODE = Pattern.compile(
            "(.+?)[.\\s_-]+season[.\\s_-]*(\\d{1,2})[.\\s_-]+episode[.\\s_-]*(\\d{1,3}).*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ParseResult tryParse(ParsingContext context) {
        String fileName = context.getFileNameWithoutExt();
        Matcher matcher = TV_SEASON_EPISODE.matcher(fileName);

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
                    .confidence(0.85)
                    .parserName(getName())
                    .build();

            log.debug("电视剧解析成功 (season.episode): {} -> 剧名={}, Season {} Episode {}",
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
        return 80; // 高优先级
    }

    @Override
    public boolean canParse(ParsingContext context) {
        // 快速预检查: 文件名包含 season 和 episode 关键词
        String fileName = context.getFileNameWithoutExt().toLowerCase();
        return fileName.contains("season") && fileName.contains("episode");
    }
}
