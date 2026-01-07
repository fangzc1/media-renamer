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
 * 仅集数格式电视剧解析器
 *
 * 支持格式:
 * - 01 _ 郭女俠怒砸同福店.mkv
 * - 第01集 剧集标题.mkv
 * - 001.mkv
 *
 * 注意: 需要从目录名推断剧集名称
 */
@Slf4j
@Component
public class EpisodeOnlyTvShowParser implements MediaParser {

    // 仅集数格式: 匹配集数和可选的剧集标题
    private static final Pattern TV_EPISODE_ONLY = Pattern.compile(
            "^(?:第)?(\\d{1,3})(?:集)?[\\s_-]*(.*)$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ParseResult tryParse(ParsingContext context) {
        String fileName = context.getFileNameWithoutExt();
        String seriesName = context.getDetectedSeriesName();

        // 剧集名不能为空
        if (seriesName == null || seriesName.trim().isEmpty()) {
            return ParseResult.failure();
        }

        Matcher matcher = TV_EPISODE_ONLY.matcher(fileName);
        if (matcher.matches()) {
            try {
                int episode = Integer.parseInt(matcher.group(1));
                String episodeTitle = matcher.group(2).trim();

                String title = seriesName
                        .replaceAll("[.\\s_-]+", " ")
                        .trim();

                // 尝试从父目录提取季信息
                Integer season = extractSeasonFromDirectory(context.getParentDirectory());
                if (season == null) {
                    season = 1; // 默认第1季
                }

                ParseResult result = ParseResult.builder()
                        .successful(true)
                        .mediaType("TV_SHOW")
                        .title(title)
                        .season(season)
                        .episode(episode)
                        .confidence(0.6) // 置信度较低 (依赖目录推断)
                        .parserName(getName())
                        .build();

                if (!episodeTitle.isEmpty()) {
                    log.debug("电视剧解析成功 (仅集数): {} -> 剧名={}, S{}E{}, 剧集标题={}",
                            fileName, title, season, episode, episodeTitle);
                } else {
                    log.debug("电视剧解析成功 (仅集数): {} -> 剧名={}, S{}E{}",
                            fileName, title, season, episode);
                }

                return result;
            } catch (NumberFormatException e) {
                log.warn("解析集数失败: {}", fileName);
            }
        }

        return ParseResult.failure();
    }

    /**
     * 从目录名提取季信息
     */
    private Integer extractSeasonFromDirectory(String directoryName) {
        if (directoryName == null || directoryName.trim().isEmpty()) {
            return null;
        }

        SeasonDirectoryMatcher.SeasonInfo seasonInfo =
                SeasonDirectoryMatcher.parseSeasonFolder(directoryName);

        return seasonInfo != null ? seasonInfo.getSeasonNumber() : null;
    }

    @Override
    public String getSupportedType() {
        return "TV_SHOW";
    }

    @Override
    public int getPriority() {
        return 30; // 较低优先级 (兜底解析器)
    }

    @Override
    public boolean canParse(ParsingContext context) {
        // 必须有推断的剧集名称
        if (context.getDetectedSeriesName() == null ||
            context.getDetectedSeriesName().trim().isEmpty()) {
            return false;
        }

        // 快速预检查: 文件名以数字开头
        String fileName = context.getFileNameWithoutExt();
        return fileName.matches("^(?:第)?\\d{1,3}.*");
    }
}
