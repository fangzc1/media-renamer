<template>
  <div class="season-header" @click="toggleExpand">
    <div class="season-left">
      <el-icon class="expand-icon">
        <ArrowRight />
      </el-icon>
      <el-icon class="folder-icon">
        <Folder />
      </el-icon>
      <span class="season-title">{{ season.displayName }}</span>
    </div>

    <div class="season-right">
      <!-- Step 3 预览模式：显示详细统计信息 -->
      <template v-if="!showMatchInfo && (season.totalEpisodes > 0 || season.matchedCount)">
        <span class="episode-stats">
          {{ season.matchedCount }} / {{ season.totalEpisodes }} 集
        </span>
        <el-tag v-if="season.isComplete" type="success" effect="plain" round size="small">
          ✅ 完整
        </el-tag>
        <el-tag v-else-if="season.missingCount > 0" type="warning" effect="plain" round size="small">
          ⚠️ 缺失 {{ season.missingCount }} 集
        </el-tag>
      </template>
      <!-- 默认：显示集数统计 -->
      <span v-else class="episode-count">共 {{ season.items.length }} 集</span>

      <!-- PC 端：季度层级按钮 -->
      <template v-if="showMatchInfo && !isMobile">
        <el-button
          size="small"
          class="season-search-button"
          @click.stop="handleSeasonSearch"
        >
          手动搜索
        </el-button>
        <el-button
          size="small"
          class="season-edit-button"
          @click.stop="handleEditSeason"
        >
          <el-icon><Edit /></el-icon>
          修改季号
        </el-button>
      </template>

      <!-- 移动端："更多"按钮 -->
      <el-button
        v-if="showMatchInfo && isMobile"
        circle
        class="mobile-more-btn"
        @click.stop="handleMobileMore"
      >
        <el-icon :size="20"><MoreFilled /></el-icon>
      </el-button>
    </div>

    <!-- 移动端操作菜单 -->
    <FileActionMenu
      v-if="isMobile"
      v-model:visible="showMobileMenu"
      level="season"
      :context="season"
      @action="handleMenuAction"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowRight, Folder, Search, Edit, MoreFilled } from '@element-plus/icons-vue'
import { useMobileDetection } from '@/composables/useMobileDetection'
import FileActionMenu from './mobile/FileActionMenu.vue'

// Props
defineProps({
  season: {
    type: Object,
    required: true
    // {
    //   seasonNumber: number,
    //   displayName: string,
    //   items: Array
    // }
  },
  isExpanded: {
    type: Boolean,
    default: true
  },
  // 是否显示匹配信息 (Step 2)
  showMatchInfo: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['toggle', 'season-search', 'edit-season'])

// 移动端检测
const { isMobile } = useMobileDetection()

// 移动端菜单状态
const showMobileMenu = ref(false)

const toggleExpand = () => {
  emit('toggle')
}

// 处理季度层级搜索
const handleSeasonSearch = () => {
  emit('season-search')
}

// 处理编辑季号
const handleEditSeason = () => {
  emit('edit-season')
}

// 打开移动端菜单
const handleMobileMore = () => {
  showMobileMenu.value = true
}

// 处理移动端菜单操作
const handleMenuAction = ({ type }) => {
  if (type === 'manual-search') {
    handleSeasonSearch()
  } else if (type === 'edit-season') {
    handleEditSeason()
  }
}
</script>

<style scoped>
/* 季度标题栏 */
.season-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 36px;
  padding: 0 16px;
  background: var(--el-fill-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s;
}

.season-header:hover {
  background-color: var(--el-fill-color-light);
}

/* 左侧 */
.season-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expand-icon {
  transition: transform 0.3s;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.season-header.is-expanded .expand-icon {
  transform: rotate(90deg);
}

.folder-icon {
  color: var(--el-color-warning);
  font-size: 16px;
}

.season-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

/* 右侧 */
.season-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.episode-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.episode-stats {
  font-size: 13px;
  color: var(--el-text-color-regular);
  font-weight: 500;
  margin-right: 4px;
}

.season-search-button {
  background: transparent;
  border: 1px solid var(--el-border-color-light);
  color: var(--el-text-color-secondary);
  transition: all 0.2s;
}

.season-search-button:hover {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.season-edit-button {
  background: transparent;
  border: 1px solid var(--el-border-color-light);
  color: var(--el-text-color-secondary);
  transition: all 0.2s;
}

.season-edit-button:hover {
  background: var(--el-color-warning-light-9);
  border-color: var(--el-color-warning);
  color: var(--el-color-warning);
}

/* 移动端"更多"按钮 */
@media (max-width: 768px) {
  .mobile-more-btn {
    width: 44px;
    height: 44px;
    padding: 0;
    background: transparent;
    border: none;
    color: var(--el-text-color-secondary);
    flex-shrink: 0;
  }

  .mobile-more-btn:active {
    background-color: var(--el-fill-color-light);
  }
}
</style>
