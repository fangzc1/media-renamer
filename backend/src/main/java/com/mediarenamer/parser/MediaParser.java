package com.mediarenamer.parser;

/**
 * 媒体文件解析器接口
 *
 * 定义统一的解析器规范,支持插件化扩展
 * 实现类负责特定格式的文件名解析逻辑
 */
public interface MediaParser {

    /**
     * 尝试解析文件名
     *
     * @param context 解析上下文,包含文件名和目录信息
     * @return 解析结果,如果无法解析则返回失败结果
     */
    ParseResult tryParse(ParsingContext context);

    /**
     * 获取支持的媒体类型
     *
     * @return MOVIE, TV_SHOW 或 MIXED
     */
    String getSupportedType();

    /**
     * 获取解析器优先级
     * 优先级高的解析器会优先尝试
     *
     * @return 优先级值,越大越优先 (建议范围: 1-100)
     */
    int getPriority();

    /**
     * 判断是否可以解析该上下文
     * 用于快速预检查,避免不必要的正则匹配
     *
     * @param context 解析上下文
     * @return true 如果可以尝试解析
     */
    boolean canParse(ParsingContext context);

    /**
     * 获取解析器名称
     * 用于日志和调试
     *
     * @return 解析器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
