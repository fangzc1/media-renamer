import request from '@/utils/request'

/**
 * 扫描目录
 */
export function scanDirectory(directory) {
  return request({
    url: '/files/scan',
    method: 'get',
    params: { directory }
  })
}

/**
 * 获取系统目录列表（用于目录选择器）
 */
export function getSystemDirectories(path) {
  return request({
    url: '/files/directories',
    method: 'get',
    params: { path }
  })
}

/**
 * 搜索电影
 */
export function searchMovie(query, year) {
  return request({
    url: '/tmdb/search/movie',
    method: 'get',
    params: { query, year }
  })
}

/**
 * 获取电影详情
 */
export function getMovieDetails(movieId) {
  return request({
    url: `/tmdb/movie/${movieId}`,
    method: 'get'
  })
}

/**
 * 搜索电视剧
 */
export function searchTvShow(query, year) {
  return request({
    url: '/tmdb/search/tv',
    method: 'get',
    params: { query, year }
  })
}

/**
 * 获取电视剧详情
 */
export function getTvShowDetails(tvId) {
  return request({
    url: `/tmdb/tv/${tvId}`,
    method: 'get'
  })
}

/**
 * 生成电影重命名预览
 */
export function previewMovieRename(data) {
  return request({
    url: '/rename/preview/movie',
    method: 'post',
    data
  })
}

/**
 * 生成电视剧重命名预览
 */
export function previewTvRename(data) {
  return request({
    url: '/rename/preview/tv',
    method: 'post',
    data
  })
}

/**
 * 执行批量重命名
 */
export function executeRename(data) {
  return request({
    url: '/rename/execute',
    method: 'post',
    data
  })
}

/**
 * 整理未处理的文件
 */
export function organizeUnprocessed(data) {
  return request({
    url: '/rename/organize-unprocessed',
    method: 'post',
    data
  })
}

/**
 * 批量匹配媒体信息 (使用虚拟线程高性能处理)
 */
export function batchMatch(data) {
  return request({
    url: '/batch/match',
    method: 'post',
    data
  })
}

/**
 * 批量生成重命名预览 (使用虚拟线程高性能处理)
 */
export function batchPreviewRename(data) {
  return request({
    url: '/rename/preview/batch',
    method: 'post',
    data
  })
}

// ==================== 扫描目录配置 ====================

/**
 * 获取所有扫描目录配置
 */
export function getAllScanDirectories() {
  return request({
    url: '/scan-directories',
    method: 'get'
  })
}

/**
 * 获取启用的扫描目录
 */
export function getEnabledScanDirectories() {
  return request({
    url: '/scan-directories/enabled',
    method: 'get'
  })
}

/**
 * 添加扫描目录配置
 */
export function addScanDirectory(data) {
  return request({
    url: '/scan-directories',
    method: 'post',
    data
  })
}

/**
 * 更新扫描目录配置
 */
export function updateScanDirectory(id, data) {
  return request({
    url: `/scan-directories/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除扫描目录配置
 */
export function deleteScanDirectory(id) {
  return request({
    url: `/scan-directories/${id}`,
    method: 'delete'
  })
}

/**
 * 记录使用次数
 */
export function recordUsage(id) {
  return request({
    url: `/scan-directories/${id}/usage`,
    method: 'post'
  })
}
