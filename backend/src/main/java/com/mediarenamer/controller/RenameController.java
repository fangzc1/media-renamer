package com.mediarenamer.controller;

import com.mediarenamer.model.Result;
import com.mediarenamer.model.dto.RenamePreviewDTO;
import com.mediarenamer.model.dto.TmdbMovieDTO;
import com.mediarenamer.model.dto.TmdbTvShowDTO;
import com.mediarenamer.model.dto.VideoFileDTO;
import com.mediarenamer.service.OrganizationService;
import com.mediarenamer.service.RenameService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 重命名控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/rename")
@RequiredArgsConstructor
public class RenameController {

    private final RenameService renameService;
    private final OrganizationService organizationService;

    /**
     * 生成电影重命名预览
     */
    @PostMapping("/preview/movie")
    public Result<RenamePreviewDTO> previewMovieRename(@RequestBody MovieRenameRequest request) {
        try {
            log.info("生成电影重命名预览: file={}, template={}",
                    request.getVideoFile().getFilePath(), request.getTemplate());

            RenameService.MovieTemplate template = RenameService.MovieTemplate.valueOf(request.getTemplate());
            RenamePreviewDTO preview = renameService.generateMovieRename(
                    request.getVideoFile(),
                    request.getMovieInfo(),
                    template
            );

            return Result.success(preview);
        } catch (Exception e) {
            log.error("生成电影重命名预览失败", e);
            return Result.error("生成预览失败: " + e.getMessage());
        }
    }

    /**
     * 生成电视剧重命名预览
     */
    @PostMapping("/preview/tv")
    public Result<RenamePreviewDTO> previewTvRename(@RequestBody TvRenameRequest request) {
        try {
            log.info("生成电视剧重命名预览: file={}, template={}",
                    request.getVideoFile().getFilePath(), request.getTemplate());

            RenameService.TvTemplate template = RenameService.TvTemplate.valueOf(request.getTemplate());
            RenamePreviewDTO preview = renameService.generateTvRename(
                    request.getVideoFile(),
                    request.getTvInfo(),
                    template
            );

            return Result.success(preview);
        } catch (Exception e) {
            log.error("生成电视剧重命名预览失败", e);
            return Result.error("生成预览失败: " + e.getMessage());
        }
    }

    /**
     * 执行批量重命名
     */
    @PostMapping("/execute")
    public Result<List<RenamePreviewDTO>> executeRename(@RequestBody ExecuteRenameRequest request) {
        try {
            log.info("执行批量重命名: count={}, scanRoot={}",
                request.getPreviews().size(), request.getScanRoot());

            // 执行重命名
            List<RenamePreviewDTO> results = renameService.executeRename(
                request.getPreviews(),
                request.getScanRoot()
            );

            long successCount = results.stream().filter(r -> "success".equals(r.getStatus())).count();
            long failedCount = results.stream().filter(r -> "failed".equals(r.getStatus())).count();

            return Result.success(
                    String.format("重命名完成: 成功 %d, 失败 %d", successCount, failedCount),
                    results
            );
        } catch (Exception e) {
            log.error("执行批量重命名失败", e);
            return Result.error("重命名失败: " + e.getMessage());
        }
    }

    /**
     * 整理未处理的文件
     * 将扫描根目录下未被重命名的文件移动到 "未整理" 目录
     */
    @PostMapping("/organize-unprocessed")
    public Result<String> organizeUnprocessed(@RequestBody OrganizeRequest request) {
        try {
            log.info("整理未处理文件: scanRoot={}, resultsCount={}",
                    request.getScanRoot(), request.getRenameResults().size());

            organizationService.organizeUnprocessedFiles(
                    request.getScanRoot(),
                    request.getRenameResults()
            );

            return Result.success("未整理文件已移动到 \"未整理\" 目录");
        } catch (Exception e) {
            log.error("整理未处理文件失败", e);
            return Result.error("整理失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成重命名预览
     * 使用虚拟线程并发处理，大幅提升性能
     */
    @PostMapping("/preview/batch")
    public Result<BatchRenameResponse> batchPreview(@RequestBody BatchRenameRequest request) {
        try {
            log.info("批量生成预览请求: count={}", request.getRequests().size());

            BatchRenameResponse response = renameService.batchGeneratePreview(request);

            log.info("批量生成预览完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                    response.getSummary().getTotal(),
                    response.getSummary().getSuccess(),
                    response.getSummary().getFailed(),
                    response.getSummary().getDurationMs());

            return Result.success(
                    String.format("预览生成完成: 成功 %d, 失败 %d",
                            response.getSummary().getSuccess(),
                            response.getSummary().getFailed()),
                    response
            );
        } catch (Exception e) {
            log.error("批量生成预览失败", e);
            return Result.error("批量生成预览失败: " + e.getMessage());
        }
    }

    @Data
    public static class MovieRenameRequest {
        private VideoFileDTO videoFile;
        private TmdbMovieDTO movieInfo;
        private String template;
    }

    @Data
    public static class TvRenameRequest {
        private VideoFileDTO videoFile;
        private TmdbTvShowDTO tvInfo;
        private String template;
    }

    @Data
    public static class OrganizeRequest {
        private String scanRoot;
        private List<RenamePreviewDTO> renameResults;
    }

    /**
     * 执行重命名请求
     */
    @Data
    public static class ExecuteRenameRequest {
        private List<RenamePreviewDTO> previews;
        private String scanRoot; // 扫描根目录，用于自动整理
    }

    /**
     * 批量重命名请求
     */
    @Data
    public static class BatchRenameRequest {
        private List<SingleRenameRequest> requests;
        private Integer maxConcurrency; // 最大并发数，默认 50
    }

    /**
     * 单个重命名请求
     */
    @Data
    public static class SingleRenameRequest {
        private VideoFileDTO videoFile;
        private Object matchedInfo; // TmdbMovieDTO 或 TmdbTvShowDTO
        private String template;
        private String mediaType; // "MOVIE" 或 "TV_SHOW"
    }

    /**
     * 批量重命名响应
     */
    @Data
    @lombok.Builder
    public static class BatchRenameResponse {
        private List<RenamePreviewDTO> previews;
        private BatchSummary summary;
    }

    /**
     * 批量处理摘要
     */
    @Data
    @lombok.Builder
    public static class BatchSummary {
        private Integer total;      // 总数
        private Integer success;    // 成功数
        private Integer failed;     // 失败数
        private Long durationMs;    // 耗时（毫秒）
    }

}
