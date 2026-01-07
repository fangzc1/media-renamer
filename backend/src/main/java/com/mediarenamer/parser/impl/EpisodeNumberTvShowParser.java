package com.mediarenamer.parser.impl;

import com.mediarenamer.parser.MediaParser;
import com.mediarenamer.parser.ParseResult;
import com.mediarenamer.parser.ParsingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * E## 格式电视剧解析器 (文件名包含剧名)
 *
 * 支持格式:
 * - 不眠日.E01.mp4
 * - ShowName.EP03.mkv
 * - Show.E10.1080p.mkv
 */
@Slf4j
@Component
public class EpisodeNumberTvShowParser implements MediaParser {

    // E## 或 EP## 格式
    private static final Pattern TV_E_FORMAT = Pattern.compile(
            "(.+?)[.\\s_-]+(?:EP?)(\\d{1,3}).*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ParseResult tryParse(ParsingContext context) {
        String fileName = context.getFileNameWithoutExt();
        Matcher matcher = TV_E_FORMAT.matcher(fileName);

        if (matcher.matches()) {
            try {
                String title = matcher.group(1)
                        .replaceAll("[.\\s_-]+", " ")
                        .trim();
                int episode = Integer.parseInt(matcher.group(2));

                ParseResult result = ParseResult.builder()
                        .successful(true)
                        .mediaType("TV_SHOW")
                        .title(title)
                        .season(1) // 默认第1季
                        .episode(episode)
                        .confidence(0.75)
                        .parserName(getName())
                        .build();

                log.debug("电视剧解析成功 (E##): {} -> 剧名={}, E{}",
                        fileName, title, episode);

                return result;
            } catch (NumberFormatException e) {
                log.warn("解析集数失败: {}", fileName);
            }
        }

        return ParseResult.failure();
    }

    @Override
    public String getSupportedType() {
        return "TV_SHOW";
    }

    @Override
    public int getPriority() {
        return 70; // 中高优先级
    }

    @Override
    public boolean canParse(ParsingContext context) {
        // 快速预检查: 文件名包含 E## 或 EP##
        String fileName = context.getFileNameWithoutExt().toLowerCase();
        return fileName.matches(".*\\be\\d{1,3}\\b.*") ||
               fileName.matches(".*\\bep\\d{1,3}\\b.*");
    }
}
