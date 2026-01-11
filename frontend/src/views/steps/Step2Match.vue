<template>
  <div class="step-container">
    <!-- 1. 顶部工具栏 (整合进度显示) -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="toolbar-title">
          <el-icon class="title-icon"><Search /></el-icon>
          <span>匹配媒体信息</span>
        </div>
        <!-- 新增:简洁的进度提示 -->
        <div v-if="videoFiles.length > 0 && !batchMatching" class="match-badge">
          <el-tag :type="allMatched ? 'success' : 'warning'" size="small" effect="plain" round>
            {{ allMatched ? '全部匹配完成' : `进度: ${matchedCount}/${videoFiles.length}` }}
          </el-tag>
        </div>
      </div>

      <div class="toolbar-actions">
        <!-- 智能匹配按钮 -->
        <el-button
          type="primary"
          size="default"
          @click="handleBatchMatch"
          :loading="batchMatching"
          class="batch-button"
        >
          <span v-if="!batchMatching">匹配</span>
          <span v-else>智能匹配中...</span>
        </el-button>

        <!-- 展开/折叠切换按钮 (移动端) -->
        <el-button
          v-if="groupedFiles.length > 0 && !batchMatching && isMobile"
          circle
          class="action-icon-button"
          @click="toggleExpandState"
        >
          <el-icon :size="20">
            <Fold v-if="defaultExpanded" />
            <Expand v-else />
          </el-icon>
        </el-button>

        <!-- 展开/折叠切换按钮 (PC端) -->
        <el-button
          v-if="groupedFiles.length > 0 && !batchMatching && !isMobile"
          class="toggle-expand-button"
          @click="toggleExpandState"
        >
          <el-icon>
            <Fold v-if="defaultExpanded" />
            <Expand v-else />
          </el-icon>
          <span>{{ defaultExpanded ? '全部折叠' : '全部展开' }}</span>
        </el-button>
      </div>
    </div>

    <!-- 2. 表格区域 (占据剩余所有空间) -->
    <div class="content-wrapper">
      <!-- 批量匹配加载状态 -->
      <div v-if="batchMatching" class="loading-container">
        <div class="matching-animation">
          <el-icon class="search-icon bounce-icon" :size="48"><Loading /></el-icon>
          <div class="progress-text">
            正在智能匹配... {{ matchedCount }}/{{ videoFiles.length }}
          </div>
          <el-progress
            :percentage="matchProgress"
            :stroke-width="8"
            class="match-progress"
          />
        </div>
      </div>

      <!-- 分组卡片列表 -->
      <div v-else class="groups-container">
        <div class="groups-scroll">
          <FileGroupItem
            v-for="(group, index) in groupedFiles"
            :key="index"
            :group="group"
            :show-match-info="true"
            :default-expanded="defaultExpanded"
            :is-selection-mode="isSelectionMode"
            :selected-file-ids="selectedFileIds"
            :ref="(el) => setGroupRef(index, el)"
            @toggle-file-selection="toggleFileSelection"
            @row-click="handleRowClick"
            @group-search="handleGroupSearch"
            @season-search="handleSeasonSearch"
            @edit-season="handleEditSeason"
            @edit-file-season="handleEditFileSeason"
          >
            <template #actions="{ item, index: itemIndex }">
              <!-- PC 端：文件级手动搜索按钮 -->
              <el-button
                v-if="!isMobile"
                size="small"
                class="search-button"
                @click.stop="handleManualSearchClick(item, itemIndex)"
              >
                手动搜索
              </el-button>
            </template>
          </FileGroupItem>

          <!-- 空状态 -->
          <el-empty
            v-if="groupedFiles.length === 0"
            description="暂无数据"
          />
        </div>
      </div>
    </div>

    <!-- 3. 底部浮动操作栏 (PC端) -->
    <transition name="slide-up">
      <div v-if="isSelectionMode && selectedFileIds.size > 0 && !isMobile" class="floating-action-bar">
        <div class="action-bar-left">
          <el-icon class="check-icon"><Select /></el-icon>
          <span class="selected-count">已选择 {{ selectedFileIds.size }} 个文件</span>
        </div>

        <div class="action-bar-right">
          <el-button size="default" @click="handleReplaceKeyword">
            <el-icon><Edit /></el-icon>
            替换关键字
          </el-button>
          <el-button size="default" @click="handleBatchModifySeason">
            <el-icon><Edit /></el-icon>
            修改季号
          </el-button>
          <el-button type="primary" size="default" @click="handleBatchManualMatch">
            <el-icon><MagicStick /></el-icon>
            统一匹配媒体信息
          </el-button>
        </div>
      </div>
    </transition>

    <!-- 4. 移动端底部批量操作栏 -->
    <transition name="slide-up">
      <div v-if="isSelectionMode && selectedFileIds.size > 0 && isMobile" class="mobile-action-bar">
        <div class="bar-left" @click="unselectAll">
          <el-icon class="close-icon"><Close /></el-icon>
          <span class="count">已选 {{ selectedFileIds.size }} 项</span>
        </div>

        <div class="bar-right">
          <el-button circle @click="showBatchMenu = true" class="more-button">
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <el-button type="primary" round @click="handleBatchManualMatch" class="match-button">
            <el-icon><MagicStick /></el-icon>
            统一匹配
          </el-button>
        </div>
      </div>
    </transition>

    <!-- 批量搜索对话框 -->
    <SearchDialog
      v-model:visible="showBatchSearchDialog"
      :current-file="selectedFiles[0]"
      @select-result="handleBatchSelectResult"
    />

    <!-- 层级搜索对话框 -->
    <SearchDialog
      v-model:visible="showHierarchicalSearchDialog"
      :current-file="hierarchicalSearchContext?.group?.items?.[0] || hierarchicalSearchContext?.season?.items?.[0]"
      @select-result="handleHierarchicalSelectResult"
    />

    <!-- 替换关键字对话框 -->
    <el-dialog
      v-model="showReplaceDialog"
      title="批量替换文件名关键字"
      width="500px"
    >
      <el-form label-width="100px">
        <el-form-item label="查找内容">
          <el-input
            v-model="replaceFrom"
            placeholder="输入要替换的文本"
            clearable
          />
        </el-form-item>
        <el-form-item label="替换为">
          <el-input
            v-model="replaceTo"
            placeholder="输入新文本(留空表示删除)"
            clearable
          />
        </el-form-item>
        <el-alert
          title="此操作将临时修改原始文件名并重新触发匹配，不会修改磁盘文件。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>

      <template #footer>
        <el-button @click="showReplaceDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmReplace">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改季号对话框 -->
    <ModifySeasonDialog
      v-model:visible="showModifySeasonDialog"
      :current-season="modifySeasonContext.currentSeason"
      :count="modifySeasonContext.count"
      :context-type="modifySeasonContext.type"
      @confirm="handleModifySeasonConfirm"
    />

    <!-- 移动端批量操作菜单 -->
    <BottomSheet v-model="showBatchMenu" title="批量操作" size="small">
      <div class="action-list">
        <div class="action-item" @click="handleReplaceKeyword">
          <el-icon class="action-icon"><Edit /></el-icon>
          <span class="action-text">替换关键字</span>
        </div>
        <div class="action-item" @click="handleBatchModifySeason">
          <el-icon class="action-icon"><Edit /></el-icon>
          <span class="action-text">修改季号</span>
        </div>
      </div>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox, ElButton } from 'element-plus'
import {
  Search,
  Loading,
  Expand,
  Fold,
  Edit,
  MagicStick,
  InfoFilled,
  Close,
  MoreFilled,
  Select
} from '@element-plus/icons-vue'
import { batchMatch } from '@/api/media'
import FileGroupItem from '@/components/FileGroupItem.vue'
import SearchDialog from '@/components/SearchDialog.vue'
import ModifySeasonDialog from '@/components/dialogs/ModifySeasonDialog.vue'
import BottomSheet from '@/components/mobile/BottomSheet.vue'
import { groupFilesBySeriesName } from '@/utils/grouping'
import { useMobileDetection } from '@/composables/useMobileDetection'

// Props
const props = defineProps({
  videoFiles: {
    type: Array,
    default: () => []
  }
})

// Emits
const emit = defineEmits([
  'update:videoFiles',
  'update:canNext',
  'update:loading',
  'manual-search'
])

// 移动端检测
const { isMobile } = useMobileDetection()

// 数据
const batchMatching = ref(false)

// 选择模式相关状态
const selectedFileIds = ref(new Set()) // 选中的文件 ID 集合
const isSelectionMode = computed(() => selectedFileIds.value.size > 0) // 自动激活选择模式

// 上一次选中的文件 ID (用于 Shift 范围选择)
const lastSelectedFileId = ref(null)

// 视觉顺序的文件列表 (用于 Shift 范围选择)
const visualFiles = computed(() => {
  const files = []
  if (groupedFiles.value) {
    groupedFiles.value.forEach(group => {
      if (group.seasons) {
        group.seasons.forEach(season => {
          if (season.items) files.push(...season.items)
        })
      } else if (group.items) {
        files.push(...group.items)
      }
    })
  }
  return files
})

// 分组数据
const groupedFiles = computed(() => {
  return groupFilesBySeriesName(props.videoFiles)
})

// 选中的文件列表
const selectedFiles = computed(() => {
  return props.videoFiles.filter(f => selectedFileIds.value.has(f.id))
})

// 是否有未匹配的选中项
const hasUnmatchedSelection = computed(() => {
  return selectedFiles.value.some(f => !f.matchedInfo)
})

// 未匹配的文件列表
const unmatchedFiles = computed(() => {
  return props.videoFiles.filter(f => !f.matchedInfo)
})

// 匹配进度
const matchedCount = computed(() => {
  return props.videoFiles.filter(f => f.matchedInfo != null).length
})

const matchProgress = computed(() => {
  if (props.videoFiles.length === 0) return 0
  return Math.round((matchedCount.value / props.videoFiles.length) * 100)
})

// 是否有已匹配的文件（至少1个）
const hasMatchedFiles = computed(() => {
  return props.videoFiles.length > 0 &&
    props.videoFiles.some(f => f.matchedInfo != null)
})

// 是否全部匹配（用于显示状态）
const allMatched = computed(() => {
  return props.videoFiles.length > 0 &&
    props.videoFiles.every(f => f.matchedInfo != null)
})

// 默认展开状态
const defaultExpanded = ref(true)

// 存储所有分组组件的引用
const groupRefs = ref([])

const setGroupRef = (index, el) => {
  if (el) {
    groupRefs.value[index] = el
  }
}

// 全部展开
const expandAll = () => {
  defaultExpanded.value = true
  groupRefs.value.forEach(group => {
    if (group && group.expand) {
      group.expand()
    }
  })
}

// 全部折叠
const collapseAll = () => {
  defaultExpanded.value = false
  groupRefs.value.forEach(group => {
    if (group && group.collapse) {
      group.collapse()
    }
  })
}

// 切换展开/折叠状态
const toggleExpandState = () => {
  if (defaultExpanded.value) {
    collapseAll()
  } else {
    expandAll()
  }
}

// 切换单个文件的选中状态
const toggleFileSelection = (fileId) => {
  if (selectedFileIds.value.has(fileId)) {
    selectedFileIds.value.delete(fileId)
  } else {
    selectedFileIds.value.add(fileId)
  }
  // 触发响应式更新
  selectedFileIds.value = new Set(selectedFileIds.value)
}

// 全选
const selectAll = () => {
  selectedFileIds.value = new Set(props.videoFiles.map(f => f.id))
}

// 取消全选
const unselectAll = () => {
  selectedFileIds.value.clear()
  selectedFileIds.value = new Set(selectedFileIds.value)
}


// 处理行点击 (支持 Shift/Ctrl)
const handleRowClick = (item, event) => {
  if (!item) return

  const fileId = item.id

  // 1. Shift + Click (范围选择)
  if (event.shiftKey && lastSelectedFileId.value) {
    const lastIndex = visualFiles.value.findIndex(f => f.id === lastSelectedFileId.value)
    const currentIndex = visualFiles.value.findIndex(f => f.id === fileId)

    if (lastIndex !== -1 && currentIndex !== -1) {
      const start = Math.min(lastIndex, currentIndex)
      const end = Math.max(lastIndex, currentIndex)

      const newSelection = new Set(selectedFileIds.value)

      // 如果没按 Ctrl/Cmd，清除之前的选择
      if (!event.ctrlKey && !event.metaKey) {
        newSelection.clear()
      }

      for (let i = start; i <= end; i++) {
        newSelection.add(visualFiles.value[i].id)
      }

      selectedFileIds.value = newSelection
    }
    return
  }

  // 2. Ctrl/Cmd + Click (多选)
  if (event.ctrlKey || event.metaKey) {
    toggleFileSelection(fileId)
    lastSelectedFileId.value = fileId
    return
  }

  // 3. 普通点击
  // 移动端: 进入选择模式后,点击切换单个项的选中状态
  if (isMobile.value) {
    if (isSelectionMode.value) {
      // 已经在选择模式中: 切换当前项的选中状态
      toggleFileSelection(fileId)
      lastSelectedFileId.value = fileId
    } else {
      // 首次点击: 选中当前项,进入选择模式
      selectedFileIds.value = new Set([fileId])
      lastSelectedFileId.value = fileId
    }
  } else {
    // PC 端: 保持原有行为,直接选中单个
    selectedFileIds.value = new Set([fileId])
    lastSelectedFileId.value = fileId
  }
}

// 键盘事件处理
const handleKeydown = (e) => {
  // Ctrl+A / Cmd+A
  if ((e.ctrlKey || e.metaKey) && e.key === 'a') {
    e.preventDefault()
    selectAll()
  }
  // Esc
  if (e.key === 'Escape') {
    if (selectedFileIds.value.size > 0) {
      unselectAll()
    }
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})


// 监听状态变化，通知父组件
watch(batchMatching, (val) => {
  emit('update:loading', val)
})

// 只要有至少1个文件匹配成功就允许进入下一步
watch(hasMatchedFiles, (val) => {
  emit('update:canNext', val)
}, { immediate: true })

// 处理手动搜索点击
const handleManualSearchClick = (file, index) => {
  emit('manual-search', { file, index })
}

// 批量自动匹配
const handleBatchMatch = async () => {
  batchMatching.value = true

  try {
    // 调用后端批量匹配 API（使用虚拟线程并发处理）
    const res = await batchMatch({
      videoFiles: props.videoFiles
    })

    // 更新匹配结果
    emit('update:videoFiles', res.data)

    const matchedCount = res.data.filter(f => f.matchedInfo != null).length
    ElMessage.success(`⚡ 批量匹配完成! 成功匹配 ${matchedCount} 个文件 ✨`)
  } catch (error) {
    console.error('批量匹配失败:', error)
    ElMessage.error('批量匹配失败: ' + (error.message || '未知错误'))
  } finally {
    batchMatching.value = false
  }
}

// 批量手动匹配（统一匹配媒体信息）
const showBatchSearchDialog = ref(false)

const handleBatchManualMatch = () => {
  showBatchSearchDialog.value = true
}

// 层级搜索相关
const hierarchicalSearchContext = ref(null) // { type: 'group' | 'season', group, season }
const showHierarchicalSearchDialog = ref(false)

// 剧集层级搜索
const handleGroupSearch = (group) => {
  hierarchicalSearchContext.value = {
    type: 'group',
    group: group
  }
  showHierarchicalSearchDialog.value = true
}

// 季度层级搜索
const handleSeasonSearch = ({ group, season }) => {
  hierarchicalSearchContext.value = {
    type: 'season',
    group: group,
    season: season
  }
  showHierarchicalSearchDialog.value = true
}

// 层级搜索结果选择
const handleHierarchicalSelectResult = (tmdbMedia) => {
  const context = hierarchicalSearchContext.value
  if (!context) return

  // 获取需要更新的文件列表
  let targetFiles = []

  if (context.type === 'group') {
    // 剧集层级: 应用到该组下所有文件
    targetFiles = context.group.items
  } else if (context.type === 'season') {
    // 季度层级: 应用到该季度下所有文件
    targetFiles = context.season.items
  }

  // 提取目标文件的 ID
  const targetFileIds = new Set(targetFiles.map(f => f.id))

  // 更新这些文件的媒体信息
  const updatedFiles = props.videoFiles.map(f => {
    if (targetFileIds.has(f.id)) {
      return {
        ...f,
        matchedInfo: {
          tmdbId: tmdbMedia.id,
          title: tmdbMedia.title || tmdbMedia.name,
          originalTitle: tmdbMedia.originalTitle || tmdbMedia.originalName,
          year: tmdbMedia.year,
          mediaType: f.mediaType,
          posterPath: tmdbMedia.posterPath
        }
      }
    }
    return f
  })

  emit('update:videoFiles', updatedFiles)

  const typeText = context.type === 'group' ? '剧集组' : '季度'
  ElMessage.success(`✅ 已为${typeText}下的 ${targetFiles.length} 个文件设置媒体信息`)

  showHierarchicalSearchDialog.value = false
  hierarchicalSearchContext.value = null
}

// 批量搜索结果选择
const handleBatchSelectResult = (tmdbMedia) => {
  // 更新选中文件的媒体信息
  const updatedFiles = props.videoFiles.map(f => {
    if (selectedFileIds.value.has(f.id)) {
      // 更新为选定的媒体信息
      return {
        ...f,
        matchedInfo: {
          tmdbId: tmdbMedia.id,
          title: tmdbMedia.title || tmdbMedia.name,
          originalTitle: tmdbMedia.originalTitle || tmdbMedia.originalName,
          year: tmdbMedia.year,
          mediaType: f.mediaType,
          posterPath: tmdbMedia.posterPath
        }
      }
    }
    return f
  })

  emit('update:videoFiles', updatedFiles)
  ElMessage.success(`✅ 已为 ${selectedFileIds.value.size} 个文件设置媒体信息`)
  unselectAll()
  showBatchSearchDialog.value = false
}

// 替换关键字
const showReplaceDialog = ref(false)
const replaceFrom = ref('')
const replaceTo = ref('')

// 移动端批量操作菜单
const showBatchMenu = ref(false)

// 修改季号
const showModifySeasonDialog = ref(false)
const modifySeasonContext = ref({
  type: 'file', // 'season' | 'file' | 'batch'
  currentSeason: null,
  count: 0,
  targetFileIds: [] // 受影响的文件 ID 列表
})

const handleReplaceKeyword = () => {
  showBatchMenu.value = false // 关闭移动端菜单
  showReplaceDialog.value = true
  replaceFrom.value = ''
  replaceTo.value = ''
}

const confirmReplace = () => {
  if (!replaceFrom.value) {
    ElMessage.warning('请输入要替换的文本')
    return
  }

  const updatedFiles = props.videoFiles.map(f => {
    if (selectedFileIds.value.has(f.id)) {
      // 替换文件名中的关键字
      const newFileName = f.fileName.replace(new RegExp(replaceFrom.value, 'g'), replaceTo.value)
      return {
        ...f,
        fileName: newFileName,
        parsedTitle: f.parsedTitle?.replace(new RegExp(replaceFrom.value, 'g'), replaceTo.value)
      }
    }
    return f
  })

  emit('update:videoFiles', updatedFiles)
  ElMessage.success(`✅ 已替换 ${selectedFileIds.value.size} 个文件的关键字`)
  showReplaceDialog.value = false
  unselectAll()
}

// 处理季度层级编辑季号
const handleEditSeason = ({ group, season }) => {
  modifySeasonContext.value = {
    type: 'season',
    currentSeason: season.seasonNumber,
    count: season.items.length,
    targetFileIds: season.items.map(item => item.id)
  }
  showModifySeasonDialog.value = true
}

// 处理文件级编辑季号
const handleEditFileSeason = (item) => {
  modifySeasonContext.value = {
    type: 'file',
    currentSeason: item.parsedSeason,
    count: 1,
    targetFileIds: [item.id]
  }
  showModifySeasonDialog.value = true
}

// 处理批量修改季号
const handleBatchModifySeason = () => {
  if (selectedFileIds.value.size === 0) {
    ElMessage.warning('请先选择文件')
    return
  }

  showBatchMenu.value = false // 关闭移动端菜单

  // 获取选中文件的季号（如果全部相同则显示,否则为 null）
  const selectedFilesArray = props.videoFiles.filter(f => selectedFileIds.value.has(f.id))
  const seasons = [...new Set(selectedFilesArray.map(f => f.parsedSeason))]
  const currentSeason = seasons.length === 1 ? seasons[0] : null

  modifySeasonContext.value = {
    type: 'batch',
    currentSeason: currentSeason,
    count: selectedFileIds.value.size,
    targetFileIds: Array.from(selectedFileIds.value)
  }
  showModifySeasonDialog.value = true
}

// 确认修改季号
const handleModifySeasonConfirm = (newSeason) => {
  const targetIds = new Set(modifySeasonContext.value.targetFileIds)

  // 更新文件的 parsedSeason
  const updatedFiles = props.videoFiles.map(f => {
    if (targetIds.has(f.id)) {
      return {
        ...f,
        parsedSeason: newSeason
      }
    }
    return f
  })

  emit('update:videoFiles', updatedFiles)

  const contextTypeText = {
    'season': '整季',
    'file': '单文件',
    'batch': '批量'
  }[modifySeasonContext.value.type]

  ElMessage.success(`✅ ${contextTypeText}季号已修改为 Season ${newSeason}，共 ${modifySeasonContext.value.count} 个文件`)

  // 清空选择
  if (modifySeasonContext.value.type === 'batch') {
    unselectAll()
  }

  showModifySeasonDialog.value = false
}

</script>

<style scoped>
/* 根容器：占满 100% 高度 */
.step-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ==================== 卡片化布局 ==================== */
/* 外层包装：创建卡片效果 */
.step-container {
  background: var(--surface-1);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-subtle);
}

/* 顶部工具栏：融合进卡片顶部 */
.toolbar {
  height: 64px;
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--space-xl);
  border-bottom: 1px solid var(--surface-2);
  background: var(--surface-1);
  border-radius: var(--radius-md) var(--radius-md) 0 0; /* 顶部圆角 */
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  letter-spacing: 0.01em;
}

.title-icon {
  color: var(--primary-brand);
  font-size: 20px;
}

.match-badge {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.info-icon {
  color: var(--el-color-warning);
  font-size: 16px;
  cursor: help;
  transition: color 0.2s;
}

.info-icon:hover {
  color: var(--el-color-warning-dark-2);
}

.toolbar-actions {
  display: flex;
  gap: var(--space-md);
  align-items: center; /* 确保垂直居中 */
}

/* PC端展开/折叠切换按钮 */
.toggle-expand-button {
  border: none;
  background-color: rgba(30, 136, 229, 0.08); /* 极浅品牌色背景 */
  color: var(--primary-color, #1E88E5); /* 品牌色文字 */
  border-radius: 8px; /* 匹配移动端圆角风格 */
  transition: all 0.2s;
  padding: 8px 16px;
  font-size: 14px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.toggle-expand-button:hover {
  background-color: rgba(30, 136, 229, 0.15); /* Hover 加深 */
  color: var(--primary-color-dark, #1976D2);
}

.toggle-expand-button:active {
  background-color: rgba(30, 136, 229, 0.2); /* 点击时更深 */
}

/* ==================== 内容区域 ==================== */
.content-wrapper {
  flex: 1;
  overflow: hidden;
  position: relative;
  padding: 0;
}

/* 加载状态 */
.loading-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--surface-1);
}

.matching-animation {
  text-align: center;
}

.search-icon {
  color: var(--primary-brand);
  margin-bottom: var(--space-lg);
}

.progress-text {
  font-size: 15px;
  color: var(--text-secondary);
  margin-bottom: var(--space-xl);
}

.match-progress {
  max-width: 400px;
  margin: 0 auto;
}

/* ==================== 表格样式 ==================== */
/* 分组容器 */
.groups-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.groups-scroll {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
  background: var(--surface-1);
}

/* 手动搜索按钮 */
.search-button {
  background: transparent;
  border: 1px solid var(--el-border-color-light);
  color: var(--el-text-color-secondary);
  transition: all 0.2s;
}

.search-button:hover {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

/* ==================== 底部浮动操作栏 ==================== */
.floating-action-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color);
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--space-xl);
  z-index: 100;
}

.action-bar-left {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.check-icon {
  color: var(--el-color-primary);
  font-size: 20px;
}

.selected-count {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.action-bar-right {
  display: flex;
  gap: var(--space-md);
}

/* 浮动栏滑入动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s ease-out;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

/* ==================== 移动端适配 ==================== */
@media (max-width: 768px) {
  .toolbar {
    height: 56px;
    padding: 0 var(--space-lg); /* 减少左右内边距 24px -> 16px */
  }

  .toolbar-left {
    gap: var(--space-sm); /* 减少左侧间距 16px -> 8px */
  }

  .toolbar-actions {
    gap: 4px; /* 优化：从 var(--space-sm) 8px 减少到 4px */
  }

  /* 确保按钮高度一致，避免视觉偏差 */
  .batch-button {
    height: 32px;
    display: inline-flex;
    align-items: center;
  }

  .action-icon-button {
    width: 32px;
    height: 32px;
    padding: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: none;
    background: transparent;
  }

  .toolbar-title {
    font-size: 15px;
  }

  /* 调整标题图标大小 */
  .title-icon {
    font-size: 18px;
  }

  /* 隐藏 PC 端的浮动操作栏 */
  .floating-action-bar {
    display: none;
  }
}

/* ==================== 移动端批量操作栏 ==================== */
.mobile-action-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color);
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--mobile-gutter);
  z-index: 100;
  /* iOS 安全区域适配 */
  padding-bottom: env(safe-area-inset-bottom);
}

.bar-left {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

.close-icon {
  color: var(--text-secondary);
  font-size: 20px;
}

.count {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-main);
}

.bar-right {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.more-button {
  width: var(--touch-target-min);
  height: var(--touch-target-min);
  background: var(--surface-2);
  border: none;
  color: var(--text-secondary);
}

.match-button {
  height: var(--touch-target-min);
  padding: 0 var(--space-lg);
  font-size: 14px;
  font-weight: 500;
}

/* ==================== 移动端批量操作菜单 ==================== */
.action-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.action-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg) 0;
  border-bottom: 1px solid var(--border-subtle);
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  transition: background-color 0.2s;
}

.action-item:last-child {
  border-bottom: none;
}

.action-item:active {
  background-color: var(--surface-2);
}

.action-icon {
  color: var(--text-secondary);
  font-size: 20px;
}

.action-text {
  font-size: var(--mobile-font-base);
  color: var(--text-main);
  font-weight: 500;
}

</style>
