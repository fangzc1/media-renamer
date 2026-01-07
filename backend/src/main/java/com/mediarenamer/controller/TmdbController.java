package com.mediarenamer.controller;

import com.mediarenamer.model.Result;
import com.mediarenamer.model.dto.TmdbMovieDTO;
import com.mediarenamer.model.dto.TmdbTvShowDTO;
import com.mediarenamer.service.TmdbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TMDB API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tmdb")
@RequiredArgsConstructor
public class TmdbController {

    private final TmdbService tmdbService;

    /**
     * 搜索电影
     *
     * @param query 搜索关键词
     * @param year  年份 (可选)
     * @return 电影列表
     */
    @GetMapping("/search/movie")
    public Result<List<TmdbMovieDTO>> searchMovie(
            @RequestParam String query,
            @RequestParam(required = false) Integer year) {
        try {
            log.info("搜索电影: query={}, year={}", query, year);
            List<TmdbMovieDTO> movies = tmdbService.searchMovie(query, year);
            return Result.success(movies);
        } catch (Exception e) {
            log.error("搜索电影失败", e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取电影详情
     *
     * @param movieId 电影 ID
     * @return 电影详情
     */
    @GetMapping("/movie/{movieId}")
    public Result<TmdbMovieDTO> getMovieDetails(@PathVariable Long movieId) {
        try {
            log.info("获取电影详情: movieId={}", movieId);
            TmdbMovieDTO movie = tmdbService.getMovieDetails(movieId);
            return Result.success(movie);
        } catch (Exception e) {
            log.error("获取电影详情失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 搜索电视剧
     *
     * @param query 搜索关键词
     * @param year  年份 (可选)
     * @return 电视剧列表
     */
    @GetMapping("/search/tv")
    public Result<List<TmdbTvShowDTO>> searchTvShow(
            @RequestParam String query,
            @RequestParam(required = false) Integer year) {
        try {
            log.info("搜索电视剧: query={}, year={}", query, year);
            List<TmdbTvShowDTO> tvShows = tmdbService.searchTvShow(query, year);
            return Result.success(tvShows);
        } catch (Exception e) {
            log.error("搜索电视剧失败", e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取电视剧详情
     *
     * @param tvId 电视剧 ID
     * @return 电视剧详情
     */
    @GetMapping("/tv/{tvId}")
    public Result<TmdbTvShowDTO> getTvShowDetails(@PathVariable Long tvId) {
        try {
            log.info("获取电视剧详情: tvId={}", tvId);
            TmdbTvShowDTO tvShow = tmdbService.getTvShowDetails(tvId);
            return Result.success(tvShow);
        } catch (Exception e) {
            log.error("获取电视剧详情失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

}
