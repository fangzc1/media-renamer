<template>
  <!-- 桌面端: el-dialog, 移动端: el-drawer -->
  <component
    :is="isMobile ? 'el-drawer' : 'el-dialog'"
    v-model="dialogVisible"
    :title="isMobile ? undefined : '📂 扫描目录管理'"
    :width="isMobile ? '100%' : '900px'"
    :size="isMobile ? '100%' : undefined"
    :close-on-click-modal="false"
    :direction="isMobile ? 'rtl' : undefined"
    class="directory-manager-container"
  >
    <!-- 移动端头部 -->
    <template v-if="isMobile" #header>
      <div class="mobile-header">
        <h3>📂 扫描目录管理</h3>
      </div>
    </template>

    <!-- 桌面端添加表单 -->
    <el-card v-if="!isMobile" shadow="never" class="add-card">
      <el-form :model="newDirectory" label-width="100px" size="default">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="目录名称">
              <el-input v-model="newDirectory.name" placeholder="如: 我的电影库" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="媒体类型">
              <el-select v-model="newDirectory.mediaType" placeholder="选择类型">
                <el-option label="🎬 电影" value="MOVIE" />
                <el-option label="📺 电视剧" value="TV_SHOW" />
                <el-option label="📁 混合" value="MIXED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="目录路径">
              <el-popover
                ref="directoryPopover"
                placement="bottom-start"
                :width="500"
                trigger="click"
              >
                <template #reference>
                  <el-input
                    v-model="newDirectory.path"
                    placeholder="请选择或输入目录路径"
                  >
                    <template #suffix>
                      <el-icon class="el-input__icon" style="cursor: pointer;">
                        <Folder />
                      </el-icon>
                    </template>
                  </el-input>
                </template>

                <!-- 目录树区域 -->
                <div class="directory-tree-container" style="max-height: 400px; overflow-y: auto;">
                  <el-tree
                    :props="treeProps"
                    :load="loadDirectoryNode"
                    lazy
                    highlight-current
                    @node-click="handleNodeClick"
                  />
                </div>
              </el-popover>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input
                v-model="newDirectory.description"
                type="textarea"
                :rows="2"
                placeholder="选填"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" @click="handleAdd" :loading="adding">
            添加目录
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 目录列表 -->
    <div class="list-container" :style="isMobile ? 'margin-top: 0' : 'margin-top: 20px'">
      <!-- 桌面端: Table -->
      <el-table v-if="!isMobile" :data="directories" style="width: 100%">
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="path" label="路径" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.mediaType === 'MOVIE' ? 'warning' : row.mediaType === 'TV_SHOW' ? 'success' : 'info'"
              size="small"
            >
              {{ row.mediaType === 'MOVIE' ? '电影' : row.mediaType === 'TV_SHOW' ? '电视剧' : '混合' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="使用次数" width="100">
          <template #default="{ row }">
            {{ row.usageCount || 0 }} 次
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-popconfirm
              title="确定删除此目录配置吗?"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" size="small" link>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 移动端: Card List -->
      <div v-else class="mobile-card-list">
        <div v-if="directories.length === 0" class="empty-state">
          <el-empty description="暂无扫描目录">
            <el-button type="primary" @click="openAddForm">添加第一个目录</el-button>
          </el-empty>
        </div>
        <div v-for="item in directories" :key="item.id" class="dir-card">
          <div class="card-header">
            <div class="card-title">{{ item.name }}</div>
            <el-switch
              v-model="item.enabled"
              @change="handleToggle(item)"
            />
          </div>
          <div class="card-body">
            <el-tag
              :type="item.mediaType === 'MOVIE' ? 'warning' : item.mediaType === 'TV_SHOW' ? 'success' : 'info'"
              size="small"
              class="media-type-tag"
            >
              {{ item.mediaType === 'MOVIE' ? '🎬 电影' : item.mediaType === 'TV_SHOW' ? '📺 电视剧' : '📁 混合' }}
            </el-tag>
            <div class="path-info">{{ item.path }}</div>
            <div class="usage-info">使用次数: {{ item.usageCount || 0 }} 次</div>
          </div>
          <div class="card-footer">
            <el-button type="primary" size="small" @click="handleEdit(item)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="confirmDelete(item.id)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 移动端: 浮动添加按钮 -->
    <div v-if="isMobile && directories.length > 0" class="mobile-fab">
      <el-button type="primary" circle size="large" @click="openAddForm">
        <el-icon size="20"><Plus /></el-icon>
      </el-button>
    </div>

    <!-- 桌面端底部 -->
    <template v-if="!isMobile" #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </component>

  <!-- 移动端: 添加/编辑表单 Drawer -->
  <el-drawer
    v-if="isMobile"
    v-model="formDrawerVisible"
    :title="editingDirectory ? '编辑目录' : '添加目录'"
    size="100%"
    direction="rtl"
    append-to-body
    class="mobile-form-drawer"
  >
    <el-form :model="newDirectory" label-position="top" size="large">
      <el-form-item label="目录名称">
        <el-input v-model="newDirectory.name" placeholder="如: 我的电影库" />
      </el-form-item>
      <el-form-item label="媒体类型">
        <el-select v-model="newDirectory.mediaType" placeholder="选择类型" style="width: 100%">
          <el-option label="🎬 电影" value="MOVIE" />
          <el-option label="📺 电视剧" value="TV_SHOW" />
          <el-option label="📁 混合" value="MIXED" />
        </el-select>
      </el-form-item>
      <el-form-item label="目录路径">
        <el-input
          v-model="newDirectory.path"
          placeholder="请选择或输入目录路径"
          @click="openPathSelector"
        >
          <template #suffix>
            <el-icon class="el-input__icon" style="cursor: pointer;">
              <Folder />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="备注">
        <el-input
          v-model="newDirectory.description"
          type="textarea"
          :rows="3"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleAdd" :loading="adding" style="width: 100%">
          {{ editingDirectory ? '保存' : '添加目录' }}
        </el-button>
        <el-button @click="formDrawerVisible = false" style="width: 100%; margin-top: 8px">取消</el-button>
      </el-form-item>
    </el-form>
  </el-drawer>

  <!-- 移动端: 路径选择器 Drawer -->
  <el-drawer
    v-if="isMobile"
    v-model="pathSelectorVisible"
    title="选择目录"
    size="100%"
    direction="rtl"
    append-to-body
    class="mobile-path-selector"
  >
    <div class="directory-tree-container mobile-tree">
      <el-tree
        :props="treeProps"
        :load="loadDirectoryNode"
        lazy
        highlight-current
        @node-click="handleMobileNodeClick"
      />
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getAllScanDirectories,
  addScanDirectory,
  updateScanDirectory,
  deleteScanDirectory,
  getSystemDirectories
} from '@/api/media'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'refresh'])

const dialogVisible = ref(false)
const directories = ref([])
const adding = ref(false)
const directoryPopover = ref(null)
const formDrawerVisible = ref(false)
const pathSelectorVisible = ref(false)
const editingDirectory = ref(null)

// 响应式判断
const windowWidth = ref(window.innerWidth)
const isMobile = computed(() => windowWidth.value < 768)

const handleResize = () => {
  windowWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const newDirectory = ref({
  name: '',
  path: '',
  mediaType: 'MIXED',
  description: '',
  enabled: true
})

// Tree 配置
const treeProps = {
  label: 'name',
  children: 'children',
  isLeaf: (data) => !data.hasChildren
}

// 懒加载目录节点
const loadDirectoryNode = async (node, resolve) => {
  try {
    const path = node.level === 0 ? undefined : node.data.path
    const res = await getSystemDirectories(path)

    if (res.code === 200) {
      const nodes = res.data.map(item => ({
        name: item.name,
        path: item.path,
        hasChildren: item.hasChildren,
        isWritable: item.isWritable
      }))
      resolve(nodes)
    } else {
      ElMessage.error(res.message || '加载目录失败')
      resolve([])
    }
  } catch (error) {
    console.error('加载目录失败:', error)
    ElMessage.error('加载目录失败')
    resolve([])
  }
}

// 处理树节点点击 - 桌面端
const handleNodeClick = (data) => {
  newDirectory.value.path = data.path
  if (directoryPopover.value) {
    directoryPopover.value.hide()
  }
}

// 处理树节点点击 - 移动端
const handleMobileNodeClick = (data) => {
  newDirectory.value.path = data.path
  pathSelectorVisible.value = false
}

// 打开添加表单(移动端)
const openAddForm = () => {
  editingDirectory.value = null
  resetForm()
  formDrawerVisible.value = true
}

// 打开路径选择器(移动端)
const openPathSelector = () => {
  pathSelectorVisible.value = true
}

// 监听对话框显示/隐藏
watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val) {
    loadDirectories()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

// 加载目录列表
const loadDirectories = async () => {
  try {
    const res = await getAllScanDirectories()
    directories.value = res.data
  } catch (error) {
    ElMessage.error('加载目录列表失败')
  }
}

// 添加目录
const handleAdd = async () => {
  if (!newDirectory.value.name || !newDirectory.value.path) {
    ElMessage.warning('请填写目录名称和路径')
    return
  }

  adding.value = true
  try {
    if (editingDirectory.value) {
      await updateScanDirectory(editingDirectory.value.id, newDirectory.value)
      ElMessage.success('更新成功')
    } else {
      await addScanDirectory(newDirectory.value)
      ElMessage.success('添加成功')
    }
    resetForm()
    loadDirectories()
    emit('refresh')
    if (isMobile.value) {
      formDrawerVisible.value = false
    }
  } catch (error) {
    ElMessage.error(editingDirectory.value ? '更新失败' : '添加失败')
  } finally {
    adding.value = false
  }
}

// 切换启用状态
const handleToggle = async (row) => {
  try {
    await updateScanDirectory(row.id, { enabled: row.enabled })
    ElMessage.success('更新成功')
    emit('refresh')
  } catch (error) {
    ElMessage.error('更新失败')
    row.enabled = !row.enabled // 回滚
  }
}

// 编辑
const handleEdit = (row) => {
  editingDirectory.value = row
  newDirectory.value = {
    name: row.name,
    path: row.path,
    mediaType: row.mediaType,
    description: row.description || '',
    enabled: row.enabled
  }
  if (isMobile.value) {
    formDrawerVisible.value = true
  } else {
    ElMessage.info('编辑功能开发中...')
  }
}

// 确认删除(移动端)
const confirmDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除此目录配置吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await handleDelete(id)
  } catch {
    // 用户取消
  }
}

// 删除目录
const handleDelete = async (id) => {
  try {
    await deleteScanDirectory(id)
    ElMessage.success('删除成功')
    loadDirectories()
    emit('refresh')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 重置表单
const resetForm = () => {
  newDirectory.value = {
    name: '',
    path: '',
    mediaType: 'MIXED',
    description: '',
    enabled: true
  }
  editingDirectory.value = null
}
</script>

<style scoped>
.add-card {
  margin-bottom: 20px;
}

/* 移动端样式 */
.mobile-header {
  display: flex;
  align-items: center;
  padding: 16px;
}

.mobile-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.mobile-card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.dir-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 16px;
  transition: all 0.2s;
}

.dir-card:active {
  background: var(--el-fill-color-light);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.card-body {
  margin-bottom: 12px;
}

.media-type-tag {
  margin-bottom: 8px;
}

.path-info {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 8px;
  word-break: break-all;
  line-height: 1.5;
}

.usage-info {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 6px;
}

.card-footer {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.card-footer .el-button {
  flex: 1;
}

.mobile-fab {
  position: fixed;
  right: 20px;
  bottom: 80px;
  z-index: 1000;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

/* 移动端表单样式 */
.mobile-form-drawer :deep(.el-drawer__body) {
  padding: 20px;
}

.mobile-form-drawer .el-form-item {
  margin-bottom: 20px;
}

.mobile-form-drawer .el-input,
.mobile-form-drawer .el-select,
.mobile-form-drawer .el-textarea {
  font-size: 16px;
}

.mobile-form-drawer :deep(.el-input__inner) {
  height: 44px;
  line-height: 44px;
}

/* 移动端路径选择器样式 */
.mobile-path-selector :deep(.el-drawer__body) {
  padding: 16px;
}

.mobile-tree :deep(.el-tree-node__content) {
  height: 48px;
  font-size: 15px;
}

.mobile-tree :deep(.el-tree-node__expand-icon) {
  font-size: 18px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .directory-manager-container :deep(.el-drawer__header) {
    padding: 16px;
    margin-bottom: 0;
  }

  .directory-manager-container :deep(.el-drawer__body) {
    padding: 0;
  }
}
</style>
