<template>
  <!-- 遮罩层 -->
  <transition name="fade">
    <div
      v-if="visible"
      class="search-dialog-overlay"
      @click="handleClose"
    ></div>
  </transition>

  <!-- 对话框主体 -->
  <transition name="slide-up">
    <div v-if="visible" class="search-dialog">
      <!-- 1. 顶栏：标题 + 关闭按钮 -->
      <div class="search-header">
        <h2 class="search-title">手动搜索媒体</h2>
        <button class="close-btn" @click="handleClose">
          <span class="icon-close">✕</span>
        </button>
      </div>

      <!-- 2. 搜索框：集成清除 + 搜索按钮 -->
      <div class="search-input-container">
        <input
          v-model="searchQuery"
          class="search-input"
          placeholder="输入电影或电视剧名称"
          @keyup.enter="handleSearch"
          ref="searchInputRef"
        />
        <button
          v-if="searchQuery"
          class="clear-btn"
          @click="handleClear"
          aria-label="清除输入"
        >
          ✕
        </button>
        <button
          class="search-btn"
          @click="handleSearch"
          :disabled="searching"
          aria-label="搜索"
        >
          <span v-if="!searching">🔍 搜索</span>
          <span v-else class="loading-icon">⏳</span>
        </button>
      </div>

      <!-- 3. 结果区域 -->
      <div class="search-results">
        <!-- 加载状态 -->
        <div v-if="searching" class="loading-state">
          <div class="search-animation">🔍</div>
          <p class="loading-text">正在搜索...</p>
        </div>

        <!-- 搜索结果卡片 -->
        <div v-else-if="searchResults.length > 0" class="mobile-results">
          <div
            v-for="(result, index) in searchResults"
            :key="index"
            class="result-card"
            @click="handleSelectResult(result)"
          >
            <div class="card-header">
              <div class="card-title">
                <strong>{{ result.title || result.name }}</strong>
                <div class="card-subtitle">{{ result.originalTitle || result.originalName }}</div>
              </div>
              <div class="card-meta">
                <span class="card-year">{{ result.year }}</span>
                <span class="card-rating">⭐ {{ result.voteAverage }}/10</span>
              </div>
            </div>
            <div class="card-overview">
              {{ result.overview || '暂无简介' }}
            </div>
          </div>
        </div>

        <!-- 空结果 -->
        <div v-else-if="!searching && searchQuery" class="empty-state">
          <div class="empty-icon">🎬</div>
          <h3 class="empty-title">没有找到相关结果</h3>
          <p class="empty-desc">请尝试使用其他关键词搜索</p>
          <div class="search-tips">
            <p class="tips-title">💡 搜索建议：</p>
            <ul class="tips-list">
              <li>使用电影的原始英文名称</li>
              <li>检查拼写是否正确</li>
              <li>尝试简化搜索关键词</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { searchMovie, searchTvShow } from '@/api/media'

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  currentFile: {
    type: Object,
    default: null
  }
})

// Emits
const emit = defineEmits(['update:visible', 'select-result'])

// 数据
const searchQuery = ref('')
const searching = ref(false)
const searchResults = ref([])
const searchInputRef = ref(null)

// 监听对话框打开，自动填充搜索关键词并聚焦
watch(() => props.visible, async (newVal) => {
  if (newVal) {
    // 重置状态
    searchResults.value = []

    if (props.currentFile) {
      searchQuery.value = props.currentFile.parsedTitle
      // 自动搜索
      await handleSearch()
    }

    // 聚焦输入框
    await nextTick()
    searchInputRef.value?.focus()
  }
})

// 关闭对话框
const handleClose = () => {
  emit('update:visible', false)
}

// 清除输入
const handleClear = () => {
  searchQuery.value = ''
  searchInputRef.value?.focus()
}

// 搜索
const handleSearch = async () => {
  if (!searchQuery.value) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  searching.value = true
  try {
    const file = props.currentFile
    let res
    if (file.mediaType === 'MOVIE') {
      res = await searchMovie(searchQuery.value, file.parsedYear)
    } else if (file.mediaType === 'TV_SHOW') {
      res = await searchTvShow(searchQuery.value, file.parsedYear)
    }

    searchResults.value = res.data
    if (res.data.length === 0) {
      ElMessage.info('没有找到匹配结果，请尝试其他关键词')
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败: ' + (error.message || '未知错误'))
  } finally {
    searching.value = false
  }
}

// 选择搜索结果
const handleSelectResult = (row) => {
  emit('select-result', row)
  emit('update:visible', false)
  ElMessage.success('匹配成功! 🎯')
}
</script>

<style scoped>
/* ==================== 遮罩层 ==================== */
.search-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  backdrop-filter: blur(4px);
}

/* 遮罩淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ==================== 对话框主体 ==================== */
.search-dialog {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  top: 0;
  background: var(--surface-1);
  z-index: 2001;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 对话框滑入动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

/* ==================== 顶栏 ==================== */
.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-subtle);
  flex-shrink: 0;
  background: var(--surface-1);
}

.search-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
}

.close-btn {
  min-width: 44px;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s ease;
  border-radius: var(--radius-md);
  padding: 0;
}

.close-btn:hover {
  background: var(--surface-2);
  color: var(--text-main);
}

.close-btn:active {
  transform: scale(0.95);
}

.icon-close {
  font-size: 24px;
  line-height: 1;
}

/* ==================== 搜索框区域 ==================== */
.search-input-container {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-bottom: 1px solid var(--border-subtle);
  flex-shrink: 0;
  background: var(--surface-1);
}

.search-input {
  flex: 1;
  min-height: 48px;
  padding: 12px 16px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  font-size: 16px;
  color: var(--text-main);
  background: var(--surface-1);
  outline: none;
  transition: all 0.2s ease;
}

.search-input:focus {
  border-color: var(--primary-brand);
  box-shadow: 0 0 0 3px var(--primary-surface);
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

/* 清除按钮 */
.clear-btn {
  min-width: 44px;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--text-tertiary);
  font-size: 20px;
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.clear-btn:hover {
  background: var(--surface-2);
  color: var(--text-secondary);
}

.clear-btn:active {
  transform: scale(0.95);
}

/* 搜索按钮 */
.search-btn {
  min-width: 88px;
  min-height: 48px;
  background: var(--primary-brand);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.search-btn:hover {
  background: var(--primary-hover);
  box-shadow: var(--shadow-hover);
}

.search-btn:active {
  transform: scale(0.98);
}

.search-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* ==================== 结果区域 ==================== */
.search-results {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  background: var(--app-bg);
}

/* ==================== 加载状态 ==================== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.search-animation {
  font-size: 48px;
  margin-bottom: 16px;
  animation: search-pulse 1.5s ease-in-out infinite;
}

@keyframes search-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.7;
  }
}

.loading-text {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0;
}

/* ==================== 结果列表 ==================== */
.mobile-results {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.result-card {
  background: var(--surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  -webkit-tap-highlight-color: transparent;
}

.result-card:active {
  background: var(--surface-2);
  transform: scale(0.98);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 12px;
}

.card-title {
  flex: 1;
  min-width: 0;
}

.card-title strong {
  font-size: 15px;
  color: var(--text-main);
  display: block;
  margin-bottom: 4px;
  word-wrap: break-word;
}

.card-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  word-wrap: break-word;
}

.card-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.card-year {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  background: var(--surface-2);
  padding: 2px 8px;
  border-radius: 4px;
}

.card-rating {
  font-size: 13px;
  color: #f7ba2a;
  font-weight: 600;
}

.card-overview {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ==================== 空结果状态 ==================== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 32px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 24px 0;
  line-height: 1.6;
}

.search-tips {
  background: var(--surface-2);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  max-width: 320px;
  width: 100%;
}

.tips-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0 0 12px 0;
  text-align: left;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  text-align: left;
}

.tips-list li {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.8;
}

/* ==================== 响应式优化 ==================== */
@media (max-width: 480px) {
  .search-btn {
    min-width: 72px;
    font-size: 14px;
  }

  .mobile-results {
    padding: 12px;
    gap: 10px;
  }

  .result-card {
    padding: 14px;
  }

  .empty-state {
    padding: 40px 20px;
  }
}

/* 滚动条样式 */
.search-results::-webkit-scrollbar {
  width: 6px;
}

.search-results::-webkit-scrollbar-track {
  background: transparent;
}

.search-results::-webkit-scrollbar-thumb {
  background: var(--border-focus);
  border-radius: 3px;
}

.search-results::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}
</style>
