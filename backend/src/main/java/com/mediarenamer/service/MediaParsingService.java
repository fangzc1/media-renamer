package com.mediarenamer.service;

import com.mediarenamer.parser.MediaParser;
import com.mediarenamer.parser.ParseResult;
import com.mediarenamer.parser.ParsingContext;
import com.mediarenamer.util.SeasonDirectoryMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.Comparator;
import java.util.List;

/**
 * 媒体文件解析服务
 *
 * 统一的解析入口,负责:
 * 1. 管理所有解析器
 * 2. 按优先级调度解析器
 * 3. 应用标题清洗
 * 4. 处理强制模式和自动模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaParsingService {

    private final List<MediaParser> parsers;
    private final TitleCleaningService titleCleaningService;

    /**
     * 按优先级排序的解析器列表
     */
    private List<MediaParser> sortedParsers;

    @PostConstruct
    public void init() {
        // 按优先级降序排序 (优先级高的排在前面)
        sortedParsers = parsers.stream()
                .sorted(Comparator.comparingInt(MediaParser::getPriority).reversed())
                .toList();

        log.info("已注册 {} 个解析器:", sortedParsers.size());
        for (MediaParser parser : sortedParsers) {
            log.info("  - {} (优先级={}, 类型={})",
                    parser.getName(),
                    parser.getPriority(),
                    parser.getSupportedType());
        }
    }

    /**
     * 解析媒体文件
     *
     * @param context 解析上下文
     * @return 解析结果
     */
    public ParseResult parseMediaFile(ParsingContext context) {
        log.debug("开始解析文件: {}", context.getFileName());

        ParseResult result;

        // 路径A: 强制电影模式
        if (context.isForceMovieMode()) {
            result = parseInForceMode(context, "MOVIE");
        }
        // 路径B: 强制电视剧模式
        else if (context.isForceTvShowMode()) {
            result = parseInForceMode(context, "TV_SHOW");
        }
        // 路径C: 自动模式
        else {
            result = parseInAutoMode(context);
        }

        // 应用标题清洗
        if (result.isSuccessful() && result.getTitle() != null) {
            String cleanedTitle = titleCleaningService.cleanTitle(result.getTitle());
            result.setTitle(cleanedTitle);
        }

        return result;
    }

    /**
     * 强制模式解析
     * 只使用匹配指定类型的解析器
     */
    private ParseResult parseInForceMode(ParsingContext context, String forceType) {
        log.debug("强制{}模式解析", "MOVIE".equals(forceType) ? "电影" : "电视剧");

        for (MediaParser parser : sortedParsers) {
            // 只使用匹配类型的解析器
            if (!parser.getSupportedType().equals(forceType) &&
                !"MIXED".equals(parser.getSupportedType())) {
                continue;
            }

            if (!parser.canParse(context)) {
                continue;
            }

            ParseResult result = parser.tryParse(context);
            if (result.isSuccessful()) {
                log.debug("解析成功 (强制模式): parser={}, confidence={}",
                        result.getParserName(), result.getConfidence());
                return result;
            }
        }

        // 强制模式下解析失败,返回兜底结果
        log.debug("强制模式解析失败,返回兜底结果");
        return createFallbackResult(context, forceType);
    }

    /**
     * 自动模式解析
     * 按优先级尝试所有解析器
     */
    private ParseResult parseInAutoMode(ParsingContext context) {
        log.debug("自动模式解析");

        ParseResult bestResult = null;
        double bestConfidence = 0.0;

        for (MediaParser parser : sortedParsers) {
            if (!parser.canParse(context)) {
                continue;
            }

            ParseResult result = parser.tryParse(context);
            if (result.isSuccessful()) {
                log.debug("解析候选: parser={}, confidence={}, type={}",
                        result.getParserName(),
                        result.getConfidence(),
                        result.getMediaType());

                // 记录最佳结果
                if (result.getConfidence() > bestConfidence) {
                    bestResult = result;
                    bestConfidence = result.getConfidence();
                }

                // 如果置信度很高,直接返回 (短路优化)
                if (result.getConfidence() >= 0.9) {
                    log.debug("高置信度解析成功,提前返回");
                    return result;
                }
            }
        }

        // 返回最佳结果或未知类型
        if (bestResult != null) {
            log.debug("自动模式解析成功: type={}, confidence={}",
                    bestResult.getMediaType(), bestResult.getConfidence());
            return bestResult;
        }

        log.debug("自动模式解析失败,返回未知类型");
        return ParseResult.unknown(context.getFileNameWithoutExt());
    }

    /**
     * 创建兜底结果 (强制模式解析失败时使用)
     */
    private ParseResult createFallbackResult(ParsingContext context, String mediaType) {
        Integer defaultSeason = null;

        // 只有在电视剧模式且有上下文支持时才设置季数
        if ("TV_SHOW".equals(mediaType)) {
            // 优先从父目录提取季信息
            if (context.isParentIsSeasonFolder()) {
                SeasonDirectoryMatcher.SeasonInfo seasonInfo =
                        SeasonDirectoryMatcher.parseSeasonFolder(context.getParentDirectory());
                if (seasonInfo != null) {
                    defaultSeason = seasonInfo.getSeasonNumber();
                }
            }

            // 如果仍为null且有检测到的剧集名称,才使用默认值1
            if (defaultSeason == null &&
                context.getDetectedSeriesName() != null &&
                !context.getDetectedSeriesName().trim().isEmpty()) {
                defaultSeason = 1;
            }
        }

        return ParseResult.builder()
                .successful(true)
                .mediaType(mediaType)
                .title(context.getFileNameWithoutExt())
                .season(defaultSeason)
                .confidence(0.2)
                .parserName("FallbackParser")
                .build();
    }

    /**
     * 智能推断剧集名称 (从目录结构)
     *
     * @param videoFile 视频文件
     * @param scanRoot  扫描根目录
     * @return 推断的剧集名称
     */
    public String detectSeriesName(File videoFile, String scanRoot) {
        File parentDir = videoFile.getParentFile();
        if (parentDir == null) {
            return "Unknown";
        }

        String parentName = parentDir.getName();

        // 检查父目录是否为季目录
        SeasonDirectoryMatcher.SeasonInfo seasonInfo =
                SeasonDirectoryMatcher.parseSeasonFolder(parentName);

        if (seasonInfo != null) {
            log.debug("检测到季目录: {} -> Season {}",
                    parentName, seasonInfo.getSeasonNumber());

            // 尝试向上回溯一级
            File grandParentDir = parentDir.getParentFile();

            // 安全检查: 不能超出扫描根目录
            if (grandParentDir != null &&
                !parentDir.getAbsolutePath().equals(scanRoot)) {
                String seriesName = grandParentDir.getName();
                log.debug("应用回溯逻辑: {} -> {}", parentName, seriesName);
                return seriesName;
            } else {
                log.debug("无法回溯(触达边界),使用父目录: {}", parentName);
            }
        }

        // 默认返回父目录名
        return parentName;
    }

    /**
     * 判断父目录是否为季目录
     */
    public boolean isParentDirectorySeasonFolder(String parentDirName) {
        if (parentDirName == null || parentDirName.trim().isEmpty()) {
            return false;
        }
        return SeasonDirectoryMatcher.parseSeasonFolder(parentDirName) != null;
    }
}
