<template>
  <div class="group-card" :class="{ 'is-expanded': isExpanded }">
    <!-- 1. 组标题栏 -->
    <div class="group-header" @click="toggleExpand">
      <div class="header-left">
        <el-icon class="expand-icon">
          <ArrowRight/>
        </el-icon>
        <span class="series-title">{{ group.seriesName }}</span>
        <!-- 季号标签 (仅在 Step 3 预览模式显示) -->
        <el-tag v-if="showPreviewInfo && group.season" type="info" size="small" effect="plain">
          Season {{ group.season }}
        </el-tag>
      </div>

      <div class="header-right">
        <!-- Step 3 预览模式：显示集数统计 -->
        <template v-if="showPreviewInfo">
          <!-- 电视剧按季度分组时，显示季度数量 -->
          <span v-if="group.isTvShow && group.seasons" class="episode-stats">
            共 {{ group.seasons.length }} 季
          </span>
          <!-- 单季或电影：显示总集数 -->
          <span v-else-if="group.totalEpisodes > 0" class="episode-stats">
            {{ group.matchedCount }} / {{ group.totalEpisodes }} 集
          </span>
          <span v-else class="episode-stats">
            共 {{ group.items.length }} 集
          </span>

          <!-- 健康度标签 (仅在单季或电影时显示) -->
          <template v-if="!group.seasons">
            <el-tag v-if="group.isComplete" type="success" effect="plain" round size="small">
              ✅ 完整
            </el-tag>
            <el-tag v-else-if="group.missingCount > 0" type="warning" effect="plain" round size="small">
              ⚠️ 缺失 {{ group.missingCount }} 集
            </el-tag>
          </template>
        </template>

        <!-- Step 2 匹配模式：显示匹配状态 -->
        <template v-else>
          <!-- PC 端：层级手动搜索按钮 -->
          <el-button
              v-if="showMatchInfo && !isMobile"
              size="small"
              class="group-search-button"
              @click.stop="handleGroupSearch"
          >
            手动搜索
          </el-button>
          <!-- 移动端："更多"按钮 -->
          <el-button
              v-if="showMatchInfo && isMobile"
              circle
              class="mobile-more-btn"
              @click.stop="handleMobileMore('group', group)"
          >
            <el-icon :size="20">
              <MoreFilled/>
            </el-icon>
          </el-button>
        </template>
      </div>
    </div>

    <!-- 2. 内容区域 (可折叠) -->
    <el-collapse-transition>
      <div v-show="isExpanded" class="group-body">
        <!-- 电视剧: 按季度分组渲染 -->
        <template v-if="group.isTvShow && group.seasons">
          <div
              v-for="(season, seasonIndex) in group.seasons"
              :key="season.seasonNumber"
              class="season-section"
          >
            <!-- 季度标题 -->
            <SeasonHeader
                :season="season"
                :is-expanded="seasonExpandStates[seasonIndex]"
                :show-match-info="showMatchInfo"
                :class="{ 'is-expanded': seasonExpandStates[seasonIndex] }"
                @toggle="toggleSeason(seasonIndex)"
                @season-search="handleSeasonSearch(season)"
                @edit-season="handleEditSeason(season)"
            />

            <!-- 季度内的文件列表 -->
            <el-collapse-transition>
              <div v-show="seasonExpandStates[seasonIndex]" class="season-files">
                <div
                    v-for="(item, index) in season.items"
                    :key="item.filePath || index"
                    class="file-row"
                    :class="{
                    'preview-row': showPreviewInfo,
                    'is-missing': item.type === 'missing',
                    'is-selected': selectedIds.has(item.filePath) || selectedFileIds.has(item.id)
                  }"
                    @click="handleRowClick(item, $event)"
                >
                  <FileRowContent
                      :item="item"
                      :index="index"
                      :show-preview-info="showPreviewInfo"
                      :show-match-info="showMatchInfo"
                      :is-selected="selectedFileIds.has(item.id)"
                      @toggle-selection="emit('toggle-file-selection', $event)"
                  >
                    <template #actions>
                      <slot name="actions" :item="item" :index="index"></slot>
                      <!-- PC 端：文件级修改季号按钮 -->
                      <el-button
                          v-if="showMatchInfo && !isMobile"
                          size="small"
                          class="file-edit-button"
                          @click.stop="handleEditFileSeason(item)"
                      >
                        <el-icon>
                          <Edit/>
                        </el-icon>
                      </el-button>
                      <!-- 移动端："更多"按钮 -->
                      <el-button
                          v-if="showMatchInfo && isMobile"
                          circle
                          class="mobile-more-btn"
                          @click.stop="handleMobileMore('file', item)"
                      >
                        <el-icon :size="20">
                          <MoreFilled/>
                        </el-icon>
                      </el-button>
                    </template>
                  </FileRowContent>
                </div>
              </div>
            </el-collapse-transition>
          </div>
        </template>

        <!-- 电影或未分季: 平铺渲染 -->
        <template v-else>
          <div
              v-for="(item, index) in group.items"
              :key="item.filePath || index"
              class="file-row"
              :class="{
              'preview-row': showPreviewInfo,
              'is-missing': item.type === 'missing',
              'is-selected': selectedIds.has(item.filePath) || selectedFileIds.has(item.id)
            }"
              @click="handleRowClick(item, $event)"
          >
            <FileRowContent
                :item="item"
                :index="index"
                :show-preview-info="showPreviewInfo"
                :show-match-info="showMatchInfo"
                :is-selected="selectedFileIds.has(item.id)"
                @toggle-selection="emit('toggle-file-selection', $event)"
            >
              <template #actions>
                <slot name="actions" :item="item" :index="index"></slot>
                <!-- PC 端：文件级修改季号按钮 -->
                <el-button
                    v-if="showMatchInfo && !isMobile"
                    size="small"
                    class="file-edit-button"
                    @click.stop="handleEditFileSeason(item)"
                >
                  <el-icon>
                    <Edit/>
                  </el-icon>
                </el-button>
                <!-- 移动端："更多"按钮 -->
                <el-button
                    v-if="showMatchInfo && isMobile"
                    circle
                    class="mobile-more-btn"
                    @click.stop="handleMobileMore('file', item)"
                >
                  <el-icon :size="20">
                    <MoreFilled/>
                  </el-icon>
                </el-button>
              </template>
            </FileRowContent>
          </div>
        </template>
      </div>
    </el-collapse-transition>

    <!-- 移动端操作菜单 -->
    <FileActionMenu
        v-if="isMobile"
        v-model:visible="showMobileMenu"
        :level="menuLevel"
        :context="menuContext"
        @action="handleMenuAction"
    />
  </div>
</template>

<script setup>
import {ref, computed, watch} from 'vue'
import {ArrowRight, Search, Edit, MoreFilled} from '@element-plus/icons-vue'
import SeasonHeader from './SeasonHeader.vue'
import FileRowContent from './FileRowContent.vue'
import {useMobileDetection} from '@/composables/useMobileDetection'
import FileActionMenu from './mobile/FileActionMenu.vue'

// Props
const props = defineProps({
  // 分组数据
  group: {
    type: Object,
    required: true
    // {
    //   seriesName: string,
    //   isTvShow: boolean,
    //   seasons: [{seasonNumber, displayName, items}] (电视剧),
    //   items: Array (电影),
    //   season: number (可选),
    //   totalEpisodes: number (可选),
    //   matchedCount: number (可选),
    //   missingCount: number (可选),
    //   isComplete: boolean (可选)
    // }
  },
  // 是否显示匹配信息 (Step 2)
  showMatchInfo: {
    type: Boolean,
    default: false
  },
  // 是否显示预览信息 (Step 3)
  showPreviewInfo: {
    type: Boolean,
    default: false
  },
  // 默认展开状态
  defaultExpanded: {
    type: Boolean,
    default: true
  },
  // 选中的文件ID集合
  selectedFileIds: {
    type: Set,
    default: () => new Set()
  },
  // Step 3 预览模式的选中ID集合
  selectedIds: {
    type: Set,
    default: () => new Set()
  }
})

// Emits
const emit = defineEmits(['row-click', 'toggle-file-selection', 'group-search', 'season-search', 'edit-season', 'edit-file-season'])

// 移动端检测
const {isMobile} = useMobileDetection()

// 移动端菜单状态
const showMobileMenu = ref(false)
const menuLevel = ref('file')
const menuContext = ref({})

// 展开/收起状态
const isExpanded = ref(props.defaultExpanded)

// 季度展开状态
const seasonExpandStates = ref([])

// 初始化季度展开状态
watch(() => props.group.seasons, (seasons) => {
  if (seasons) {
    // 默认展开所有季度
    seasonExpandStates.value = seasons.map(() => true)
  }
}, {immediate: true})

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

const toggleSeason = (index) => {
  seasonExpandStates.value[index] = !seasonExpandStates.value[index]
}

// 暴露给父组件的方法
const expand = () => {
  isExpanded.value = true
  // 同时展开所有季度
  if (props.group.seasons) {
    seasonExpandStates.value = props.group.seasons.map(() => true)
  }
}

const collapse = () => {
  isExpanded.value = false
}

defineExpose({
  expand,
  collapse
})

// 计算组状态 (Step 2 匹配模式)
const groupStatusType = computed(() => {
  if (!props.showMatchInfo) return 'info'

  const allItems = props.group.items
  const allMatched = allItems.every(item => item.matchedInfo != null)
  if (allMatched) return 'success'

  const someMatched = allItems.some(item => item.matchedInfo != null)
  if (someMatched) return 'warning'

  return 'info'
})

const groupStatusText = computed(() => {
  if (!props.showMatchInfo) {
    const count = props.group.items.length
    return `${count} 集`
  }

  const allItems = props.group.items
  const matchedCount = allItems.filter(item => item.matchedInfo != null).length
  const total = allItems.length

  if (matchedCount === total) return '全部匹配'
  if (matchedCount === 0) return '待匹配'
  return `${matchedCount}/${total} 已匹配`
})

// 处理剧集层级搜索
const handleGroupSearch = () => {
  emit('group-search', props.group)
}

// 处理季度层级搜索
const handleSeasonSearch = (season) => {
  emit('season-search', {group: props.group, season})
}

// 处理季度层级编辑季号
const handleEditSeason = (season) => {
  emit('edit-season', {group: props.group, season})
}

// 处理文件级编辑季号
const handleEditFileSeason = (item) => {
  emit('edit-file-season', item)
}

// 处理行点击事件
const handleRowClick = (item, event) => {
  // 触发 row-click 事件，传递 item 和原始事件
  emit('row-click', item, event)
}

// 打开移动端菜单
const handleMobileMore = (level, context) => {
  menuLevel.value = level
  menuContext.value = context
  showMobileMenu.value = true
}

// 处理移动端菜单操作
const handleMenuAction = ({type, level, context}) => {
  if (type === 'manual-search') {
    if (level === 'group') {
      handleGroupSearch()
    } else if (level === 'season') {
      handleSeasonSearch(context)
    } else if (level === 'file') {
      // 文件级手动搜索，触发父组件事件
      emit('group-search', props.group) // 使用组级搜索
    }
  } else if (type === 'edit-season') {
    if (level === 'season') {
      handleEditSeason(context)
    } else if (level === 'file') {
      handleEditFileSeason(context)
    }
  }
}
</script>

<style scoped>
/* ==================== 组卡片 ==================== */
.group-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  margin-bottom: 12px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: hidden;
}

.group-card:hover {
  box-shadow: var(--el-box-shadow-light);
  border-color: var(--el-border-color);
}

/* ==================== 组标题栏 ==================== */
.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-fill-color-blank);
  cursor: pointer;
  user-select: none;
  border-bottom: 1px solid transparent;
  transition: all 0.2s;
}

.group-header:hover {
  background-color: var(--el-fill-color-light);
}

.is-expanded .group-header {
  border-bottom-color: var(--el-border-color-lighter);
  background-color: var(--el-fill-color-lighter);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
  margin-right: 8px;
}

.expand-icon {
  transition: transform 0.3s;
  color: var(--el-text-color-secondary);
  font-size: 16px;
}

.is-expanded .expand-icon {
  transform: rotate(90deg);
}

.series-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.count-badge {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  background: var(--el-fill-color);
  padding: 2px 8px;
  border-radius: 10px;
}

.episode-stats {
  font-size: 13px;
  color: var(--el-text-color-regular);
  font-weight: 500;
  margin-right: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.group-search-button {
  background: transparent;
  border: 1px solid var(--el-border-color-light);
  color: var(--el-text-color-secondary);
  transition: all 0.2s;
  margin-left: 8px;
}

.group-search-button:hover {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

/* ==================== 组内容区 ==================== */
.group-body {
  background-color: var(--el-bg-color);
  padding: 0;
}

/* ==================== 季度区域 ==================== */
.season-section {
  /* 季度区块样式 */
}

.season-files {
  /* 季度内的文件列表 */
}

/* ==================== 文件行 ==================== */
.file-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
  transition: background-color 0.2s;
  gap: 16px;
  cursor: pointer;
  user-select: none; /* 防止 Shift 选择时的文本选中 */
}

.file-row:last-child {
  border-bottom: none;
}

.file-row:hover {
  background-color: var(--el-fill-color-light);
}

/* 选中状态样式 */
.file-row.is-selected {
  background-color: var(--el-color-primary-light-9);
  border-left: 3px solid var(--el-color-primary);
}

.file-row.is-selected:hover {
  background-color: var(--el-color-primary-light-8);
}

/* 预览行需要更高的高度 */
.file-row.preview-row {
  min-height: 100px;
  align-items: stretch;
}

/* ==================== 缺失占位符样式 ==================== */
.file-row.is-missing {
  border: 1px dashed var(--el-border-color);
  background-color: var(--el-fill-color-lighter);
  opacity: 0.85;
  cursor: default;
}

.file-row.is-missing:hover {
  background-color: var(--el-fill-color-lighter);
}

/* 文件级编辑按钮 */
.file-edit-button {
  background: transparent;
  border: 1px solid var(--el-border-color-light);
  color: var(--el-text-color-secondary);
  transition: all 0.2s;
  padding: 8px;
}

.file-edit-button:hover {
  background: var(--el-color-warning-light-9);
  border-color: var(--el-color-warning);
  color: var(--el-color-warning);
}

/* 移动端"更多"按钮 */
@media (max-width: 768px) {
  /* 移动端分组标题优化 */
  .header-left {
    gap: 8px;
  }

  .series-title {
    font-size: 14px;
  }

  /* 确保标签不被压缩 */
  .header-left .el-tag {
    flex-shrink: 0;
  }

  /* 移动端文件行优化 */
  .file-row {
    padding: 10px 12px;
    gap: 8px;
  }

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
