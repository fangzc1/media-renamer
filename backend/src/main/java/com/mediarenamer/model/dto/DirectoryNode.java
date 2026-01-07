package com.mediarenamer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 目录节点 DTO
 * 用于前端目录选择器的树形展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryNode {

    /**
     * 显示名称（如 "Movies"）
     */
    private String name;

    /**
     * 完整路径（如 "/Users/username/Movies"）
     */
    private String path;

    /**
     * 是否包含子目录（用于前端懒加载判断）
     */
    private boolean hasChildren;

    /**
     * 是否可写（可选字段）
     */
    private boolean isWritable;
}
