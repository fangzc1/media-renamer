package com.mediarenamer.controller;

import com.mediarenamer.model.Result;
import com.mediarenamer.model.dto.VideoFileDTO;
import com.mediarenamer.service.BatchProcessService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 批量处理控制器
 * 使用虚拟线程实现高性能批量操作
 */
@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchProcessService batchProcessService;

    /**
     * 批量匹配媒体信息
     * 使用虚拟线程并发处理，大幅提升性能
     */
    @PostMapping("/match")
    public Result<List<VideoFileDTO>> batchMatch(@RequestBody BatchMatchRequest request) {
        try {
            log.info("批量匹配请求: files={}", request.getVideoFiles().size());

            List<VideoFileDTO> results = batchProcessService.batchMatchMedia(request.getVideoFiles());

            long matchedCount = results.stream()
                    .filter(f -> f.getMatchedInfo() != null)
                    .count();

            log.info("批量匹配完成: 总数={}, 成功={}", results.size(), matchedCount);

            return Result.success(
                    String.format("批量匹配完成: 成功匹配 %d 个文件", matchedCount),
                    results
            );
        } catch (Exception e) {
            log.error("批量匹配失败", e);
            return Result.error("批量匹配失败: " + e.getMessage());
        }
    }

    @Data
    public static class BatchMatchRequest {
        private List<VideoFileDTO> videoFiles;
    }
}
