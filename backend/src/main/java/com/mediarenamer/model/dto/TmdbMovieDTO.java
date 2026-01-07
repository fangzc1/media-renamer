package com.mediarenamer.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TMDB 电影搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略前端传入的额外字段(如 mediaType)
public class TmdbMovieDTO {

    @JsonAlias("tmdbId")  // 支持前端发送的 tmdbId 字段映射到 id
    private Long id;
    private String title;
    private String originalTitle;
    private String releaseDate;
    private Integer year;
    private String overview;
    private String posterPath;
    private String backdropPath;
    private Double voteAverage;
    private Integer voteCount;
    private List<Genre> genres;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Genre {
        private Integer id;
        private String name;
    }

}
