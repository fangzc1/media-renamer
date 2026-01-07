/**
 * 媒体文件分组工具
 * 按剧集名称将文件分组
 */

/**
 * 按剧集名称分组视频文件,并对电视剧按季度二级分组
 * @param {Array} files - 视频文件列表
 * @returns {Array} 分组后的数据 [{seriesName, isTvShow, seasons: [{seasonNumber, items}], items (电影)}]
 */
export function groupFilesBySeriesName(files) {
  if (!files || files.length === 0) {
    return []
  }

  // 使用 Map 保证顺序
  const groupMap = new Map()

  files.forEach(file => {
    // 对于电视剧和电影,优先使用匹配到的 TMDB 信息作为系列名
    // 这样可以将相同剧集的不同目录/文件归为一组
    let seriesName = '未知系列'
    const isTvShow = file.mediaType === 'TV_SHOW'

    if (file.matchedInfo) {
      // 已匹配: 优先使用 TMDB 返回的标准名称
      if (file.mediaType === 'TV_SHOW') {
        // 电视剧: 使用 TMDB 的 name 字段
        seriesName = file.matchedInfo.name || file.parsedTitle
      } else if (file.mediaType === 'MOVIE') {
        // 电影: 使用 TMDB 的 title + 年份
        const year = file.matchedInfo.releaseDate?.substring(0, 4) || file.parsedYear || '未知'
        seriesName = `${file.matchedInfo.title || file.parsedTitle} (${year})`
      }
    } else {
      // 未匹配: 降级使用解析出的信息
      if (file.mediaType === 'TV_SHOW' && file.parsedTitle) {
        seriesName = file.parsedTitle
      } else if (file.mediaType === 'MOVIE' && file.parsedTitle) {
        // 电影使用标题+年份作为唯一标识
        seriesName = `${file.parsedTitle} (${file.parsedYear || '未知'})`
      } else if (file.fileName) {
        // 如果没有解析信息,尝试从文件名提取
        seriesName = extractSeriesNameFromFileName(file.fileName)
      }
    }

    if (!groupMap.has(seriesName)) {
      groupMap.set(seriesName, {
        seriesName,
        isTvShow,
        items: [] // 用于电影或未分季的文件
      })
    }

    groupMap.get(seriesName).items.push(file)
  })

  // 转换为数组并排序
  const groups = Array.from(groupMap.values())

  // 按系列名排序
  groups.sort((a, b) => a.seriesName.localeCompare(b.seriesName))

  // 为每个组处理分季和排序
  groups.forEach(group => {
    // 先对 items 排序
    group.items.sort((a, b) => {
      // 优先按季数排序
      if (a.parsedSeason !== b.parsedSeason) {
        return (a.parsedSeason || 0) - (b.parsedSeason || 0)
      }
      // 再按集数排序
      if (a.parsedEpisode !== b.parsedEpisode) {
        return (a.parsedEpisode || 0) - (b.parsedEpisode || 0)
      }
      // 最后按文件名排序
      return a.fileName.localeCompare(b.fileName)
    })

    // 如果是电视剧,按季度分组
    if (group.isTvShow) {
      group.seasons = groupBySeasons(group.items)
    }
  })

  return groups
}

/**
 * 将电视剧文件按季度分组
 * @param {Array} items - 文件列表
 * @returns {Array} 季度分组 [{seasonNumber, items, displayName}]
 */
function groupBySeasons(items) {
  const seasonMap = new Map()

  items.forEach(item => {
    // 获取季号,未知季号设为 -1
    const seasonNumber = item.parsedSeason !== undefined && item.parsedSeason !== null
      ? item.parsedSeason
      : -1

    if (!seasonMap.has(seasonNumber)) {
      seasonMap.set(seasonNumber, {
        seasonNumber,
        items: []
      })
    }

    seasonMap.get(seasonNumber).items.push(item)
  })

  // 转换为数组并排序
  const seasons = Array.from(seasonMap.values())

  // 按季号排序:特别篇(0) -> 第1季(1) -> 第2季(2) -> ... -> 未知季(-1)
  seasons.sort((a, b) => {
    if (a.seasonNumber === -1) return 1  // 未知季排最后
    if (b.seasonNumber === -1) return -1
    return a.seasonNumber - b.seasonNumber
  })

  // 为每个季度生成显示名称
  seasons.forEach(season => {
    if (season.seasonNumber === 0) {
      season.displayName = '特别篇 (Specials)'
    } else if (season.seasonNumber === -1) {
      season.displayName = '其他 / 未知季度'
    } else {
      season.displayName = `第 ${season.seasonNumber} 季`
    }
  })

  return seasons
}

/**
 * 从文件名提取系列名称 (简单启发式方法)
 * @param {string} fileName - 文件名
 * @returns {string} 提取的系列名
 */
function extractSeriesNameFromFileName(fileName) {
  // 移除扩展名
  const nameWithoutExt = fileName.replace(/\.[^.]+$/, '')

  // 尝试移除常见的集数标识
  const patterns = [
    /[Ss]\d{1,2}[Ee]\d{1,2}.*$/, // S01E01 格式
    /第\s*\d+\s*[集季].*$/, // 中文集数
    /\d{1,2}[xX]\d{1,2}.*$/, // 1x01 格式
    /EP?\d+.*$/i // EP01 格式
  ]

  let seriesName = nameWithoutExt
  for (const pattern of patterns) {
    seriesName = seriesName.replace(pattern, '')
  }

  // 清理空格和特殊字符
  seriesName = seriesName.trim()

  return seriesName || '未知系列'
}

/**
 * 按剧集名称分组重命名预览（基于 metadata）
 * 电视剧会按季度进行二级分组
 * @param {Array} previews - 重命名预览列表
 * @returns {Array} 分组后的数据，包含缺失集数统计
 */
export function groupPreviewsBySeriesName(previews) {
  if (!previews || previews.length === 0) {
    return []
  }

  const seriesMap = new Map()

  previews.forEach(preview => {
    const metadata = preview.metadata || {}

    // 剧集名称(不含季号)
    const seriesName = metadata.seriesName || extractSeriesNameFromNewFileName(preview.pureNewFileName)

    if (!seriesMap.has(seriesName)) {
      seriesMap.set(seriesName, {
        seriesName,
        isTvShow: metadata.mediaType === 'TV', // 修复：后端返回的是 'TV' 而不是 'TV_SHOW'
        items: []
      })
    }

    seriesMap.get(seriesName).items.push(preview)
  })

  const groups = Array.from(seriesMap.values())

  // 按系列名排序
  groups.sort((a, b) => a.seriesName.localeCompare(b.seriesName))

  // 为每个组处理
  groups.forEach(group => {
    // 先排序
    group.items.sort((a, b) => {
      const aSeason = a.metadata?.seasonNumber || 0
      const bSeason = b.metadata?.seasonNumber || 0
      if (aSeason !== bSeason) {
        return aSeason - bSeason
      }

      const aEpisode = a.metadata?.episodeNumber || 0
      const bEpisode = b.metadata?.episodeNumber || 0
      if (aEpisode !== bEpisode) {
        return aEpisode - bEpisode
      }

      return a.pureNewFileName.localeCompare(b.pureNewFileName)
    })

    // 如果是电视剧,按季度分组
    if (group.isTvShow) {
      group.seasons = groupPreviewsBySeasons(group.items)
    }
  })

  return groups
}

/**
 * 将重命名预览按季度分组（用于 Step3）
 * @param {Array} items - 预览列表
 * @returns {Array} 季度分组 [{seasonNumber, displayName, items, totalEpisodes, matchedCount, missingCount, isComplete}]
 */
function groupPreviewsBySeasons(items) {
  const seasonMap = new Map()

  items.forEach(item => {
    const seasonNumber = item.metadata?.seasonNumber !== undefined
      ? item.metadata.seasonNumber
      : -1

    if (!seasonMap.has(seasonNumber)) {
      seasonMap.set(seasonNumber, {
        seasonNumber,
        items: [],
        matchedEpisodes: new Set()
      })
    }

    const season = seasonMap.get(seasonNumber)
    season.items.push(item)

    // 统计已匹配的集数
    if (item.metadata?.episodeNumber) {
      season.matchedEpisodes.add(item.metadata.episodeNumber)
    }

    // 从第一个 item 获取季度总集数
    if (!season.totalEpisodes && item.metadata?.seasonTotalEpisodes) {
      season.totalEpisodes = item.metadata.seasonTotalEpisodes
    }
  })

  const seasons = Array.from(seasonMap.values())

  // 排序
  seasons.sort((a, b) => {
    if (a.seasonNumber === -1) return 1
    if (b.seasonNumber === -1) return -1
    return a.seasonNumber - b.seasonNumber
  })

  // 为每个季度生成显示名称和统计信息
  seasons.forEach(season => {
    if (season.seasonNumber === 0) {
      season.displayName = '特别篇 (Specials)'
    } else if (season.seasonNumber === -1) {
      season.displayName = '其他 / 未知季度'
    } else {
      season.displayName = `第 ${season.seasonNumber} 季`
    }

    // 计算缺失信息
    season.matchedCount = season.matchedEpisodes.size
    season.totalEpisodes = season.totalEpisodes || 0
    season.missingCount = season.totalEpisodes > 0 ? (season.totalEpisodes - season.matchedCount) : 0
    season.isComplete = season.totalEpisodes > 0 && season.missingCount === 0

    // 插入缺失剧集占位符
    season.items = insertMissingEpisodePlaceholders(season.items, season)
  })

  return seasons
}

/**
 * 在文件列表中插入缺失剧集的占位符
 * @param {Array} items - 已排序的文件列表
 * @param {Object} season - 季度信息 (包含 seasonNumber, seriesName 等)
 * @returns {Array} 插入占位符后的列表
 */
function insertMissingEpisodePlaceholders(items, season) {
  if (!items || items.length === 0) {
    return items
  }

  const result = []
  let expectedEpisode = 1 // 从第 1 集开始期待

  items.forEach((item, index) => {
    const currentEpisode = item.metadata?.episodeNumber || 0

    // 如果没有集数信息，直接添加
    if (currentEpisode === 0) {
      result.push(item)
      return
    }

    // 如果是第一个文件，更新期待集数起点
    if (index === 0 && currentEpisode > 1) {
      expectedEpisode = 1
    }

    // 检查是否有缺失的集数
    if (currentEpisode > expectedEpisode) {
      // 生成缺失占位符
      const missingPlaceholder = createMissingPlaceholder({
        seriesName: season.seriesName || item.metadata?.seriesName || '未知剧集',
        season: season.seasonNumber !== undefined ? season.seasonNumber : 1,
        startEpisode: expectedEpisode,
        endEpisode: currentEpisode - 1
      })
      result.push(missingPlaceholder)
    }

    // 添加当前文件
    result.push(item)

    // 更新期待的下一个集数
    expectedEpisode = currentEpisode + 1
  })

  // 处理尾部缺失（如果提供了总集数）
  if (season.totalEpisodes > 0) {
    const lastEpisode = items[items.length - 1]?.metadata?.episodeNumber || 0
    if (lastEpisode > 0 && lastEpisode < season.totalEpisodes) {
      const missingPlaceholder = createMissingPlaceholder({
        seriesName: season.seriesName || items[0]?.metadata?.seriesName || '未知剧集',
        season: season.seasonNumber !== undefined ? season.seasonNumber : 1,
        startEpisode: lastEpisode + 1,
        endEpisode: season.totalEpisodes
      })
      result.push(missingPlaceholder)
    }
  }

  return result
}

/**
 * 创建缺失剧集占位符对象
 * @param {Object} params - 参数
 * @param {string} params.seriesName - 剧集名称
 * @param {number} params.season - 季号
 * @param {number} params.startEpisode - 起始集号
 * @param {number} params.endEpisode - 结束集号
 * @returns {Object} 占位符对象
 */
function createMissingPlaceholder({ seriesName, season, startEpisode, endEpisode }) {
  const count = endEpisode - startEpisode + 1
  const isSingle = count === 1

  // 格式化集号显示
  const episodeDisplay = isSingle
    ? `第 ${startEpisode} 集`
    : `第 ${startEpisode} - ${endEpisode} 集 (共 ${count} 集)`

  // 生成理论上的新文件名
  const seasonStr = String(season).padStart(2, '0')
  const episodeStr = String(startEpisode).padStart(2, '0')
  const theoreticalName = `${seriesName} - S${seasonStr}E${episodeStr}`

  return {
    type: 'missing', // 标记为缺失占位符
    season,
    startEpisode,
    endEpisode,
    count,
    seriesName,
    episodeDisplay,
    theoreticalName,
    // 为了兼容现有代码，添加空的属性
    id: `missing-${season}-${startEpisode}-${endEpisode}`,
    pureOldFileName: '',
    pureNewFileName: theoreticalName
  }
}

/**
 * 从新文件名中提取系列名
 * @param {string} newFileName - 新文件名
 * @returns {string} 系列名
 */
function extractSeriesNameFromNewFileName(newFileName) {
  // 移除扩展名
  const nameWithoutExt = newFileName.replace(/\.[^.]+$/, '')

  // 移除集数信息
  const patterns = [
    /\s*-\s*[Ss]\d{1,2}[Ee]\d{1,2}.*$/, // - S01E01 格式
    /\s*第\s*\d+\s*[集季].*$/, // 中文集数
    /\s*\d{1,2}[xX]\d{1,2}.*$/, // 1x01 格式
    /\s*EP?\d+.*$/i // EP01 格式
  ]

  let seriesName = nameWithoutExt
  for (const pattern of patterns) {
    seriesName = seriesName.replace(pattern, '')
  }

  return seriesName.trim() || '未知系列'
}
