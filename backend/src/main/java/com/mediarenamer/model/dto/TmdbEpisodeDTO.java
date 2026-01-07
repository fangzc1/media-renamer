package com.mediarenamer.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TMDB 电视剧单集信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbEpisodeDTO {

    /**
     * 集号
     */
    @JsonProperty("episode_number")
    private Integer episodeNumber;

    /**
     * 单集标题
     */
    private String name;

    /**
     * 单集概述
     */
    private String overview;

    /**
     * 播出日期
     */
    @JsonProperty("air_date")
    private String airDate;

    /**
     * 剧照路径
     */
    @JsonProperty("still_path")
    private String stillPath;

}
