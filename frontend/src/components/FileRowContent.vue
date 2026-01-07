<template>
  <!-- 集数信息 (仅在非预览模式显示) -->
  <div v-if="!showPreviewInfo" class="episode-info">
    <span
      class="episode-badge"
      :class="{
        'badge-warning': showMatchInfo && !item.matchedInfo,
        'badge-success': showMatchInfo && item.matchedInfo
      }"
    >
      {{ formatEpisodeNumber(item) }}
    </span>
  </div>

  <!-- 中间内容容器 (文件名 + 匹配信息) -->
  <div v-if="!showPreviewInfo || showMatchInfo" class="content-wrapper">
    <!-- 文件名 (仅在非预览模式显示) -->
    <div v-if="!showPreviewInfo" class="file-name">
      {{ item.fileName }}
    </div>

    <!-- 匹配信息 (Step 2) -->
    <div v-if="showMatchInfo" class="match-info">
      <div v-if="item.matchedInfo" class="matched">
        <!-- 剧集名称容器（支持移动端滚动播放） -->
        <div class="episode-title-wrapper">
          <strong
            ref="titleRef"
            class="episode-title-text"
            :class="{ overflow: isOverflow }"
            :style="{
              '--scroll-duration': scrollDuration,
              '--container-width': containerWidth
            }"
          >
            {{ item.matchedInfo.title || item.matchedInfo.name }}
          </strong>
        </div>
        <div class="rating">
          评分: {{ item.matchedInfo.voteAverage }}/10 ⭐
        </div>
      </div>
      <div v-else class="unmatched-placeholder">
        <!-- 空白占位，边框颜色在父容器体现 -->
      </div>
    </div>
  </div>

  <!-- 重命名预览 (Step 3) -->
  <div v-if="showPreviewInfo" class="preview-detail">
    <!-- 缺失占位符渲染 -->
    <template v-if="item.type === 'missing'">
      <!-- 左侧：缺失提示 -->
      <div class="missing-section">
        <div class="missing-icon">⚠️</div>
        <div class="missing-text">
          <div class="missing-label">缺失文件</div>
          <div class="missing-desc">无法在扫描目录中找到对应集数</div>
        </div>
      </div>

      <!-- 箭头（虚线） -->
      <div class="arrow-divider arrow-missing">
        <el-icon class="arrow-icon"><Right /></el-icon>
      </div>

      <!-- 右侧：理论新文件名 -->
      <div class="missing-new-section">
        <div class="theoretical-name">{{ item.theoreticalName }}</div>
        <div class="episode-badge-missing">{{ item.episodeDisplay }}</div>
      </div>
    </template>

    <!-- 正常文件预览 -->
    <template v-else>
      <!-- 原文件信息 -->
      <div class="preview-section old-section">
        <div class="section-label">原文件</div>
        <div class="path-info">
          <span class="path-label">路径:</span>
          <span class="path-value">{{ item.oldRelativeDirectory || './' }}</span>
        </div>
        <div class="file-info">
          <span class="file-label">文件名:</span>
          <span class="file-value">{{ item.pureOldFileName }}</span>
        </div>
      </div>

      <!-- 箭头 -->
      <div class="arrow-divider">
        <el-icon class="arrow-icon"><Right /></el-icon>
      </div>

      <!-- 新文件信息 -->
      <div class="preview-section new-section">
        <div class="section-label">新文件</div>
        <div class="path-info">
          <span class="path-label">路径:</span>
          <span class="path-value highlight">{{ item.newRelativeDirectory || './' }}</span>
        </div>
        <div class="file-info">
          <span class="file-label">文件名:</span>
          <span class="file-value highlight">{{ item.pureNewFileName }}</span>
        </div>
      </div>
    </template>
  </div>

  <!-- 操作按钮 -->
  <div class="actions">
    <slot name="actions"></slot>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Right } from '@element-plus/icons-vue'
import { useTextScroll } from '@/composables/useTextScroll'

// Props
const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  index: {
    type: Number,
    required: true
  },
  showPreviewInfo: {
    type: Boolean,
    default: false
  },
  showMatchInfo: {
    type: Boolean,
    default: false
  },
  isSelected: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['toggle-selection'])

// 文本滚动（移动端剧集名称）
const titleRef = ref(null)
const { isOverflow, scrollDuration, containerWidth } = useTextScroll(titleRef)

// 格式化集数
const formatEpisodeNumber = (item) => {
  if (item.parsedSeason && item.parsedEpisode) {
    const season = String(item.parsedSeason).padStart(2, '0')
    const episode = String(item.parsedEpisode).padStart(2, '0')
    return `S${season}E${episode}`
  }
  return '-'
}
</script>

<style scoped>
/* 复选框区域 */
.checkbox-wrapper {
  flex-shrink: 0;
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 集数信息 */
.episode-info {
  flex-shrink: 0;
  width: 80px;
}

/* 中间内容容器 (文件名 + 匹配信息) */
.content-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
  overflow: hidden;
}

.episode-badge {
  display: inline-block;
  padding: 4px 8px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  font-family: var(--el-font-family);
  transition: all 0.2s;
}

/* 待匹配状态 (橙色) */
.episode-badge.badge-warning {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
  border: 1px solid var(--el-color-warning);
}

/* 已匹配状态 (绿色) */
.episode-badge.badge-success {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
  border: 1px solid var(--el-color-success);
}

/* 文件名 */
.file-name {
  flex: 1;
  font-family: var(--el-font-family);
  font-size: 13px;
  color: var(--el-text-color-secondary);
  word-break: break-all;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 匹配信息 (Step 2) */
.match-info {
  flex-shrink: 0;
  width: 200px;
}

.matched {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.matched strong {
  color: var(--el-color-primary);
  font-weight: 600;
  font-size: 13px;
}

.rating {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

/* 移动端剧集名称滚动动画 */
@media (max-width: 768px) {
  /* 移动端集数信息压缩 */
  .episode-info {
    width: 60px;
  }

  .episode-badge {
    font-size: 11px;
    padding: 3px 6px;
  }

  /* 移动端内容容器垂直堆叠 */
  .content-wrapper {
    flex-direction: column;
    align-items: flex-start;
    justify-content: center;
    gap: 4px;
  }

  /* 移动端文件名占满宽度 */
  .file-name {
    width: 100%;
    font-size: 13px;
    line-height: 1.4;
    white-space: nowrap;
  }

  /* 移动端匹配信息区域宽度自适应 */
  .match-info {
    width: 100%;
    margin-top: 0;
  }

  /* 确保匹配信息内部容器宽度自适应 */
  .matched {
    width: 100%;
  }

  @keyframes text-scroll {
    0% {
      transform: translateX(0);
    }
    100% {
      transform: translateX(calc(-100% + var(--container-width, 200px)));
    }
  }

  .episode-title-wrapper {
    overflow: hidden;
    width: 100%;
    max-width: 200px;
    position: relative;
  }

  .episode-title-text {
    white-space: nowrap;
    display: inline-block;
    will-change: transform; /* GPU 加速 */
    color: var(--el-color-primary);
    font-weight: 600;
    font-size: 13px;
  }

  .episode-title-text.overflow {
    animation: text-scroll var(--scroll-duration, 10s) linear 1s infinite;
  }

  /* 移动端预览模式优化 (Step 3) */
  .preview-detail {
    flex-direction: column; /* 改为垂直排列 */
    gap: 8px; /* 减小间距 */
  }

  .preview-section {
    width: 100%; /* 占满宽度 */
    padding: 10px; /* 调整内边距 */
  }

  .arrow-divider {
    transform: rotate(90deg); /* 箭头向下旋转 */
    margin: -4px 0; /* 调整垂直间距 */
  }

  /* 优化路径和文件名的显示，允许换行但保持可读性 */
  .path-value,
  .file-value {
    white-space: normal;
    word-break: break-all;
  }
}

/* PC 端保持原样（支持换行） */
@media (min-width: 769px) {
  .episode-title-wrapper {
    width: auto;
  }

  .episode-title-text {
    word-wrap: break-word;
    white-space: normal;
  }
}

/* 预览信息 (Step 3) */
.preview-detail {
  flex: 1;
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 8px 0;
}

.preview-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}

.old-section {
  border: 1px solid var(--el-border-color-lighter);
}

.new-section {
  border: 1px solid var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
}

.section-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
}

.path-info,
.file-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 12px;
}

.path-label,
.file-label {
  flex-shrink: 0;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.path-value,
.file-value {
  flex: 1;
  color: var(--el-text-color-regular);
  font-family: var(--el-font-family);
  word-break: break-all;
}

.path-value.highlight,
.file-value.highlight {
  color: var(--el-color-primary);
  font-weight: 500;
}

.arrow-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.arrow-divider .arrow-icon {
  color: var(--el-color-primary);
  font-size: 20px;
  font-weight: bold;
}

/* 操作区域 */
.actions {
  flex-shrink: 0;
}

/* ==================== 缺失占位符样式 ==================== */
/* 缺失占位符 - 左侧提示区 */
.missing-section {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  background: var(--el-fill-color-blank);
  border: 1px dashed var(--el-border-color-light);
}

.missing-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.missing-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.missing-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-color-warning);
}

.missing-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 缺失占位符 - 箭头 */
.arrow-missing .arrow-icon {
  color: var(--el-text-color-placeholder);
  opacity: 0.5;
}

/* 缺失占位符 - 右侧理论新文件名区 */
.missing-new-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border-radius: 6px;
  background: var(--el-fill-color-blank);
  border: 1px dashed var(--el-border-color-light);
}

.theoretical-name {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  font-family: var(--el-font-family);
  text-align: center;
}

.episode-badge-missing {
  display: inline-block;
  padding: 4px 12px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
</style>
