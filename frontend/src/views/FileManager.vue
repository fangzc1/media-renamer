<template>
  <div class="file-manager-container" :class="{ 'mobile-layout': isMobile }">
    <!-- 移动端：顶部步骤导航 -->
    <MobileStepNavigator
      v-if="isMobile"
      :current-step="currentStep + 1"
      :allow-jump="true"
      @change="handleStepJump"
    />

    <!-- 主内容区域 -->
    <main class="main-content" :class="{ 'has-mobile-bottom-bar': isMobile }">
      <!-- 步骤 1: 扫描文件 -->
      <Step1Scan
        v-if="currentStep === 0"
        ref="step1Ref"
        :saved-directories="savedDirectories"
        :video-files="videoFiles"
        @update:video-files="videoFiles = $event"
        @update:scan-root-path="scanRootPath = $event"
        @update:can-next="canNext = $event"
        @update:loading="nextLoading = $event"
        @show-directory-manager="showDirectoryManager = true"
      />

      <!-- 步骤 2: 匹配媒体信息 -->
      <Step2Match
        v-if="currentStep === 1"
        ref="step2Ref"
        :video-files="videoFiles"
        @update:video-files="videoFiles = $event"
        @update:can-next="canNext = $event"
        @update:loading="nextLoading = $event"
        @manual-search="handleManualMatch"
      />

      <!-- 步骤 3: 预览重命名 -->
      <Step3Preview
        v-if="currentStep === 2"
        ref="step3Ref"
        :rename-previews="renamePreviews"
        :selected-template="selectedTemplate"
        :generating-preview="generatingPreview"
        @update:selected-template="handleTemplateChange"
        @update:can-next="canNext = $event"
        @update:loading="nextLoading = $event"
      />

      <!-- 步骤 4: 执行重命名 -->
      <Step4Execute
        v-if="currentStep === 3"
        ref="step4Ref"
        :renaming="renaming"
        :rename-complete="renameComplete"
        :renamed-count="renamedCount"
        :rename-previews="renamePreviews"
        @update:can-next="canNext = $event"
        @update:loading="nextLoading = $event"
      />
    </main>

    <!-- 桌面端：右侧边栏 -->
    <aside v-if="!isMobile" class="sidebar-right">
      <!-- 上部：步骤指示器 -->
      <div class="steps-wrapper">
        <VerticalSteps
          :current-step="currentStep"
          :steps="steps"
          @step-click="handleStepJump"
        />
      </div>

      <!-- 控制组：紧随步骤条之后 -->
      <div class="control-group">
        <el-button
          v-if="currentStep > 0"
          @click="handlePrev"
          :disabled="nextLoading"
          class="btn-prev"
        >
          <el-icon><ArrowLeft /></el-icon>
          上一步
        </el-button>
        <el-button
          type="primary"
          @click="handleNext"
          :disabled="isNextDisabled"
          :loading="nextLoading"
          class="btn-next"
        >
          {{ nextButtonText }}
          <el-icon class="el-icon--right"><component :is="nextButtonIcon" /></el-icon>
        </el-button>
      </div>
    </aside>

    <!-- 移动端：底部操作栏 -->
    <MobileBottomBar
      v-if="isMobile"
      :show-prev="currentStep > 0"
      :prev-text="'上一步'"
      :prev-disabled="nextLoading"
      :next-text="nextButtonText"
      :next-disabled="isNextDisabled"
      :loading="nextLoading"
      @prev="handlePrev"
      @next="handleNext"
    />

    <!-- 手动搜索对话框 -->
    <SearchDialog
      v-model:visible="searchDialogVisible"
      :current-file="currentSearchFile"
      @select-result="handleSelectSearchResult"
    />

    <!-- 目录管理对话框 -->
    <DirectoryManager
      v-model="showDirectoryManager"
      @refresh="loadSavedDirectories"
    />

    <!-- 进度加载动效 -->
    <ProgressLoading
      v-if="previewProgress.visible"
      :current="previewProgress.current"
      :total="previewProgress.total"
      title="正在生成重命名预览"
      progress-text="处理中"
      icon="magic"
      tip="正在调用 TMDB API 获取元数据,请稍候..."
    />
  </div>
</template>

<script setup>
import { ref, onMounted, inject, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Search } from '@element-plus/icons-vue'
import {
  previewMovieRename,
  previewTvRename,
  batchPreviewRename,
  executeRename,
  getAllScanDirectories
} from '@/api/media'
import { useMobileDetection } from '@/composables/useMobileDetection'
import Step1Scan from './steps/Step1Scan.vue'
import Step2Match from './steps/Step2Match.vue'
import Step3Preview from './steps/Step3Preview.vue'
import Step4Execute from './steps/Step4Execute.vue'
import SearchDialog from '@/components/SearchDialog.vue'
import DirectoryManager from '@/components/DirectoryManager.vue'
import VerticalSteps from '@/components/VerticalSteps.vue'
import ProgressLoading from '@/components/ProgressLoading.vue'
import MobileStepNavigator from '@/components/mobile/MobileStepNavigator.vue'
import MobileBottomBar from '@/components/mobile/MobileBottomBar.vue'

// 移动端检测
const { isMobile } = useMobileDetection()

// 注入全局步骤状态
const currentStep = inject('currentStep')

// 子组件引用
const step1Ref = ref(null)
const step2Ref = ref(null)
const step3Ref = ref(null)
const step4Ref = ref(null)

// 数据状态
const videoFiles = ref([])
const renamePreviews = ref([])
const selectedTemplate = ref('STANDARD')
const scanRootPath = ref('')

// 目录管理
const savedDirectories = ref([])
const showDirectoryManager = ref(false)

// 状态管理
const generatingPreview = ref(false)
const renaming = ref(false)
const renameComplete = ref(false)
const renamedCount = ref(0)

// 进度跟踪状态
const previewProgress = ref({
  current: 0,
  total: 0,
  visible: false
})

// 搜索对话框
const searchDialogVisible = ref(false)
const currentSearchFile = ref(null)
const currentSearchIndex = ref(-1)

// 步骤控制状态
const canNext = ref(false)
const nextLoading = ref(false)

// 步骤配置
const steps = [
  { title: '扫描文件', description: '选择媒体目录' },
  { title: '匹配媒体', description: '智能识别信息' },
  { title: '预览重命名', description: '确认命名规则' },
  { title: '执行重命名', description: '批量处理文件' }
]

// 下一步按钮文案
const nextButtonText = computed(() => {
  if (currentStep.value === 0) {
    return '开始扫描'
  }
  if (currentStep.value === 3) {
    if (renameComplete.value) {
      return '重新开始'
    }
    return '执行重命名'
  }
  return '下一步'
})

// 下一步按钮图标
const nextButtonIcon = computed(() => {
  return currentStep.value === 0 ? Search : ArrowRight
})

// 下一步按钮是否禁用
const isNextDisabled = computed(() => {
  // 步骤0（扫描阶段）：只要不在 loading 就可以点击
  if (currentStep.value === 0) {
    return nextLoading.value
  }
  // 其他步骤：根据 canNext 和 loading 状态判断
  return !canNext.value || nextLoading.value
})

// 处理上一步
const handlePrev = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 处理步骤跳转
const handleStepJump = (step) => {
  // 移动端步骤从 1 开始，需要转换为 0 开始的索引
  const targetIndex = step - 1
  // 简单的状态切换
  // 因为数据状态都在 FileManager 这一层,切换 currentStep 不会丢失 videoFiles 等数据
  currentStep.value = targetIndex
}

// 处理下一步
const handleNext = async () => {
  try {
    // 步骤 0 -> 1: 调用子组件扫描，成功后自动跳转
    if (currentStep.value === 0) {
      const success = await step1Ref.value.executeScan()
      if (success) {
        currentStep.value = 1
        ElMessage.success('扫描完成，进入匹配阶段')
      }
      return
    }

    // 步骤 1 -> 2: 生成预览
    if (currentStep.value === 1) {
      await handleGeneratePreview()
      return
    }

    // 步骤 2 -> 3: 进入执行页
    if (currentStep.value === 2) {
      currentStep.value = 3
      ElMessage.success('请确认后执行重命名')
      return
    }

    // 步骤 3: 执行重命名或重新开始
    if (currentStep.value === 3) {
      if (renameComplete.value) {
        handleReset()
      } else {
        await handleExecuteRename()
      }
    }
  } catch (error) {
    console.error('操作失败:', error)
  }
}

// 加载预设目录
const loadSavedDirectories = async () => {
  try {
    const res = await getAllScanDirectories()
    savedDirectories.value = res.data.filter(d => d.enabled)
  } catch (error) {
    console.error('加载预设目录失败:', error)
  }
}

// 手动搜索
const handleManualMatch = ({ file, index }) => {
  currentSearchFile.value = file
  currentSearchIndex.value = index
  searchDialogVisible.value = true
}

// 选择搜索结果
const handleSelectSearchResult = (row) => {
  videoFiles.value[currentSearchIndex.value].matchedInfo = row
}

// 模板变更
const handleTemplateChange = (template) => {
  selectedTemplate.value = template
  // 重新生成预览
  handleGeneratePreview()
}

// 生成预览
const handleGeneratePreview = async () => {
  generatingPreview.value = true

  // 过滤出已匹配的文件
  const matchedFiles = videoFiles.value.filter(f => f.matchedInfo)

  // 初始化进度
  previewProgress.value = {
    current: 0,
    total: matchedFiles.length,
    visible: true
  }

  try {
    // 构建批量请求
    const requests = matchedFiles.map(file => ({
      videoFile: file,
      matchedInfo: file.matchedInfo,
      template: selectedTemplate.value,
      mediaType: file.mediaType
    }))

    // 调用批量预览接口（使用虚拟线程并发处理）
    const res = await batchPreviewRename({
      requests: requests,
      maxConcurrency: 50 // 最大并发数
    })

    // 更新预览结果
    renamePreviews.value = res.data.previews

    // 显示统计信息
    const summary = res.data.summary
    ElMessage.success(
      `预览生成完成! 成功 ${summary.success} 个，失败 ${summary.failed} 个，耗时 ${summary.durationMs}ms ✨`
    )

    currentStep.value = 2
  } catch (error) {
    ElMessage.error('生成预览失败: ' + (error.message || '未知错误'))
    console.error('生成预览失败:', error)
  } finally {
    generatingPreview.value = false
    previewProgress.value.visible = false
  }
}

// 执行重命名
const handleExecuteRename = async () => {
  renaming.value = true
  renamedCount.value = 0

  try {
    // 执行重命名 (后端会自动整理未处理文件)
    const res = await executeRename({
      previews: renamePreviews.value,
      scanRoot: scanRootPath.value
    })
    renamePreviews.value = res.data
    renamedCount.value = res.data.length
    renameComplete.value = true

    // 显示重命名结果
    const successCount = res.data.filter(r => r.status === 'success').length
    const failedCount = res.data.filter(r => r.status === 'failed').length

    if (successCount > 0) {
      ElMessage.success(`重命名完成! 成功 ${successCount} 个，失败 ${failedCount} 个，未整理文件已自动归档 🎉`)
    } else {
      ElMessage.warning('所有文件重命名失败')
    }
  } catch (error) {
    console.error('重命名失败:', error)
    ElMessage.error('重命名失败')
  } finally {
    renaming.value = false
  }
}

// 重置
const handleReset = () => {
  currentStep.value = 0
  videoFiles.value = []
  renamePreviews.value = []
  renameComplete.value = false
  renamedCount.value = 0
  scanRootPath.value = ''
  canNext.value = false
  nextLoading.value = false
  ElMessage.success('已重置,可以开始新的任务! 🔄')
}

// 组件挂载时加载预设目录
onMounted(() => {
  loadSavedDirectories()
})
</script>

<style scoped>
/* 容器：左右布局 - 采用设计文档中的 app-bg */
.file-manager-container {
  height: 100%;
  display: flex;
  overflow: hidden;
  background-color: var(--app-bg); /* 使用设计 token */
}

/* 左侧：主内容区域 - 增加 padding 留白 */
.main-content {
  flex: 1;
  order: 1;
  padding: var(--space-lg); /* 使用设计 token */
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 右侧：侧边栏 - 瘦身版 */
.sidebar-right {
  width: 240px;
  order: 2;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background-color: var(--surface-1);
  border-left: 1px solid var(--border-subtle);
  padding: var(--space-xl); /* 统一上下左右的 padding */
  z-index: 10;
}

/* 步骤指示器区域 */
.steps-wrapper {
  flex: 1;
  overflow-y: auto;
}

/* 按钮组容器 */
.control-group {
  margin-top: var(--space-2xl); /* 与步骤条的间距 */
  display: flex;
  gap: var(--space-md); /* 按钮间距 */
  align-items: stretch; /* 拉伸按钮高度一致 */
}

/* 上一步按钮 */
.control-group .btn-prev {
  flex: 1; /* 1:2 比例 */
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  padding: 0 8px; /* 添加适当的内边距，避免内容太挤 */
}

/* 下一步按钮 */
.control-group .btn-next {
  flex: 2; /* 让下一步按钮更长，更易点击 */
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  padding: 0 16px; /* 较大按钮使用较大的内边距 */
  font-weight: 600;
  box-shadow: var(--shadow-hover); /* 使用设计 token */
}

/* 深色模式适配 */
[data-theme="dark"] .sidebar-right {
  border-left-color: var(--border-subtle);
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.4);
}

/* ==================== 移动端适配 ==================== */
@media (max-width: 768px) {
  .file-manager-container.mobile-layout {
    flex-direction: column;
    height: 100vh;
    overflow: hidden;
  }

  .main-content {
    flex: 1;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
    padding: var(--mobile-gutter);
  }

  .main-content.has-mobile-bottom-bar {
    padding-bottom: calc(var(--mobile-bottom-bar-height) + 16px);
  }

  /* 桌面端侧边栏在移动端隐藏（已通过 v-if 控制） */
  .sidebar-right {
    display: none;
  }
}

@media (max-width: 480px) {
  .main-content {
    padding: var(--space-sm);
  }
}
</style>
