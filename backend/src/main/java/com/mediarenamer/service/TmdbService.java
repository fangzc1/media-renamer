package com.mediarenamer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediarenamer.config.TmdbProperties;
import com.mediarenamer.model.dto.TmdbMovieDTO;
import com.mediarenamer.model.dto.TmdbTvShowDTO;
import com.mediarenamer.model.dto.TmdbEpisodeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * TMDB API 服务
 * 负责调用 TMDB API 获取电影和电视剧信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbService {

    private final RestTemplate restTemplate;
    private final TmdbProperties tmdbProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 搜索电影 (添加缓存)
     *
     * @param query 搜索关键词
     * @param year  年份 (可选)
     * @return 电影列表
     */
    @Cacheable(value = "movieSearch", key = "#query + '-' + (#year != null ? #year : 'null')")
    public List<TmdbMovieDTO> searchMovie(String query, Integer year) {
        try {
            // 构建 API URL
            String url = buildUrl("/search/movie", query, year);
            log.debug("搜索电影: query={}, year={}", query, year);

            // 调用 API
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");

            List<TmdbMovieDTO> movies = new ArrayList<>();
            if (results != null && results.isArray()) {
                for (JsonNode node : results) {
                    TmdbMovieDTO movie = parseMovieNode(node);
                    movies.add(movie);
                }
            }

            log.debug("搜索到 {} 部电影", movies.size());
            return movies;
        } catch (Exception e) {
            log.error("搜索电影失败: query={}, year={}", query, year, e);
            throw new RuntimeException("搜索电影失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取电影详情 (添加缓存)
     *
     * @param movieId 电影 ID
     * @return 电影详情
     */
    @Cacheable(value = "movieDetails", key = "#movieId")
    public TmdbMovieDTO getMovieDetails(Long movieId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(tmdbProperties.getApiUrl())
                    .pathSegment("movie", String.valueOf(movieId))
                    .queryParam("api_key", tmdbProperties.getApiKey())
                    .queryParam("language", tmdbProperties.getLanguage())
                    .toUriString();

            log.debug("获取电影详情: movieId={}", movieId);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);

            return parseMovieNode(node);
        } catch (Exception e) {
            log.error("获取电影详情失败: movieId={}", movieId, e);
            throw new RuntimeException("获取电影详情失败: " + e.getMessage(), e);
        }
    }

    /**
     * 搜索电视剧 (添加缓存)
     *
     * @param query 搜索关键词
     * @param year  年份 (可选)
     * @return 电视剧列表
     */
    @Cacheable(value = "tvSearch", key = "#query + '-' + (#year != null ? #year : 'null')")
    public List<TmdbTvShowDTO> searchTvShow(String query, Integer year) {
        try {
            String url = buildUrl("/search/tv", query, year);
            log.debug("搜索电视剧: query={}, year={}", query, year);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");

            List<TmdbTvShowDTO> tvShows = new ArrayList<>();
            if (results != null && results.isArray()) {
                for (JsonNode node : results) {
                    TmdbTvShowDTO tvShow = parseTvShowNode(node);
                    tvShows.add(tvShow);
                }
            }

            log.debug("搜索到 {} 部电视剧", tvShows.size());
            return tvShows;
        } catch (Exception e) {
            log.error("搜索电视剧失败: query={}, year={}", query, year, e);
            throw new RuntimeException("搜索电视剧失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取电视剧详情 (添加缓存)
     *
     * @param tvId 电视剧 ID
     * @return 电视剧详情
     */
    @Cacheable(value = "tvDetails", key = "#tvId")
    public TmdbTvShowDTO getTvShowDetails(Long tvId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(tmdbProperties.getApiUrl())
                    .pathSegment("tv", String.valueOf(tvId))
                    .queryParam("api_key", tmdbProperties.getApiKey())
                    .queryParam("language", tmdbProperties.getLanguage())
                    .toUriString();

            log.debug("获取电视剧详情: tvId={}", tvId);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);

            return parseTvShowNode(node);
        } catch (Exception e) {
            log.error("获取电视剧详情失败: tvId={}", tvId, e);
            throw new RuntimeException("获取电视剧详情失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取电视剧季度详情（包含集数信息）
     * 使用缓存避免重复 API 调用
     *
     * @param tvId 电视剧 ID
     * @param seasonNumber 季号
     * @return 季度的总集数，如果获取失败返回 null
     */
    @Cacheable(value = "seasonEpisodes", key = "#tvId + '-' + #seasonNumber")
    public Integer getSeasonEpisodeCount(Long tvId, Integer seasonNumber) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(tmdbProperties.getApiUrl())
                    .pathSegment("tv", String.valueOf(tvId), "season", String.valueOf(seasonNumber))
                    .queryParam("api_key", tmdbProperties.getApiKey())
                    .queryParam("language", tmdbProperties.getLanguage())
                    .toUriString();

            log.debug("获取电视剧季度详情: tvId={}, seasonNumber={}", tvId, seasonNumber);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);

            // 获取 episodes 数组的长度
            JsonNode episodesNode = node.get("episodes");
            if (episodesNode != null && episodesNode.isArray()) {
                int episodeCount = episodesNode.size();
                log.debug("季度 {} 共有 {} 集", seasonNumber, episodeCount);
                return episodeCount;
            }

            log.debug("未找到季度 {} 的集数信息", seasonNumber);
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // 404 错误是预期内的（季度可能不存在），使用 DEBUG 级别
            log.debug("季度信息不存在: tvId={}, seasonNumber={} (404 Not Found)", tvId, seasonNumber);
            return null;
        } catch (Exception e) {
            // 其他异常使用 WARN 级别（不影响主流程）
            log.warn("获取电视剧季度详情失败: tvId={}, seasonNumber={}, error={}",
                    tvId, seasonNumber, e.getMessage());
            return null;
        }
    }

    /**
     * 获取电视剧季度的所有剧集信息（包含单集标题）
     * 使用缓存避免重复 API 调用
     *
     * @param tvId 电视剧 ID
     * @param seasonNumber 季号
     * @return 该季度的所有剧集信息列表，如果获取失败返回空列表
     */
    @Cacheable(value = "seasonEpisodesDetail", key = "#tvId + '-' + #seasonNumber")
    public List<TmdbEpisodeDTO> getSeasonEpisodes(Long tvId, Integer seasonNumber) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(tmdbProperties.getApiUrl())
                    .pathSegment("tv", String.valueOf(tvId), "season", String.valueOf(seasonNumber))
                    .queryParam("api_key", tmdbProperties.getApiKey())
                    .queryParam("language", tmdbProperties.getLanguage())
                    .toUriString();

            log.debug("📡 TMDB API: /tv/{}/season/{}", tvId, seasonNumber);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);

            // 解析 episodes 数组
            JsonNode episodesNode = node.get("episodes");
            List<TmdbEpisodeDTO> episodes = new ArrayList<>();

            if (episodesNode != null && episodesNode.isArray()) {
                for (JsonNode episodeNode : episodesNode) {
                    TmdbEpisodeDTO episode = TmdbEpisodeDTO.builder()
                            .episodeNumber(getIntValue(episodeNode, "episode_number"))
                            .name(getStringValue(episodeNode, "name"))
                            .overview(getStringValue(episodeNode, "overview"))
                            .airDate(getStringValue(episodeNode, "air_date"))
                            .stillPath(getStringValue(episodeNode, "still_path"))
                            .build();
                    episodes.add(episode);
                }
                log.debug("✅ 解析成功: Season {} 共 {} 集", seasonNumber, episodes.size());
            } else {
                log.warn("⚠️ episodes 节点为空: Season {}", seasonNumber);
            }

            return episodes;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.debug("❌ 404: tvId={}, season={}", tvId, seasonNumber);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ API 失败: tvId={}, season={}, error={}",
                    tvId, seasonNumber, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 构建 API URL
     */
    private String buildUrl(String path, String query, Integer year) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(tmdbProperties.getApiUrl())
                .path(path)
                .queryParam("api_key", tmdbProperties.getApiKey())
                .queryParam("query", query)
                .queryParam("language", tmdbProperties.getLanguage());

        if (year != null) {
            if (path.contains("/movie")) {
                builder.queryParam("year", year);
            } else if (path.contains("/tv")) {
                builder.queryParam("first_air_date_year", year);
            }
        }

        return builder.toUriString();
    }

    /**
     * 解析电影节点
     */
    private TmdbMovieDTO parseMovieNode(JsonNode node) {
        String releaseDate = getStringValue(node, "release_date");
        Integer year = extractYear(releaseDate);

        List<TmdbMovieDTO.Genre> genres = new ArrayList<>();
        JsonNode genresNode = node.get("genres");
        if (genresNode != null && genresNode.isArray()) {
            for (JsonNode genreNode : genresNode) {
                genres.add(new TmdbMovieDTO.Genre(
                        genreNode.get("id").asInt(),
                        genreNode.get("name").asText()
                ));
            }
        }

        return TmdbMovieDTO.builder()
                .id(getLongValue(node, "id"))
                .title(getStringValue(node, "title"))
                .originalTitle(getStringValue(node, "original_title"))
                .releaseDate(releaseDate)
                .year(year)
                .overview(getStringValue(node, "overview"))
                .posterPath(getStringValue(node, "poster_path"))
                .backdropPath(getStringValue(node, "backdrop_path"))
                .voteAverage(getDoubleValue(node, "vote_average"))
                .voteCount(getIntValue(node, "vote_count"))
                .genres(genres)
                .build();
    }

    /**
     * 解析电视剧节点
     */
    private TmdbTvShowDTO parseTvShowNode(JsonNode node) {
        String firstAirDate = getStringValue(node, "first_air_date");
        Integer year = extractYear(firstAirDate);

        List<TmdbMovieDTO.Genre> genres = new ArrayList<>();
        JsonNode genresNode = node.get("genres");
        if (genresNode != null && genresNode.isArray()) {
            for (JsonNode genreNode : genresNode) {
                genres.add(new TmdbMovieDTO.Genre(
                        genreNode.get("id").asInt(),
                        genreNode.get("name").asText()
                ));
            }
        }

        return TmdbTvShowDTO.builder()
                .id(getLongValue(node, "id"))
                .name(getStringValue(node, "name"))
                .originalName(getStringValue(node, "original_name"))
                .firstAirDate(firstAirDate)
                .year(year)
                .overview(getStringValue(node, "overview"))
                .posterPath(getStringValue(node, "poster_path"))
                .backdropPath(getStringValue(node, "backdrop_path"))
                .voteAverage(getDoubleValue(node, "vote_average"))
                .voteCount(getIntValue(node, "vote_count"))
                .genres(genres)
                .numberOfSeasons(getIntValue(node, "number_of_seasons"))
                .numberOfEpisodes(getIntValue(node, "number_of_episodes"))
                .build();
    }

    /**
     * 从日期字符串提取年份
     */
    private Integer extractYear(String dateStr) {
        if (dateStr != null && dateStr.length() >= 4) {
            try {
                return Integer.parseInt(dateStr.substring(0, 4));
            } catch (NumberFormatException e) {
                log.warn("无法解析年份: {}", dateStr);
            }
        }
        return null;
    }

    private String getStringValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    private Long getLongValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asLong() : null;
    }

    private Integer getIntValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asInt() : null;
    }

    private Double getDoubleValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asDouble() : null;
    }

}
