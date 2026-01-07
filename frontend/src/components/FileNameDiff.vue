<template>
  <div class="filename-diff">
    <div class="diff-segment" v-for="(segment, index) in diffSegments" :key="index">
      <span
        :class="{
          'diff-same': segment.type === 'same',
          'diff-removed': segment.type === 'removed',
          'diff-added': segment.type === 'added'
        }"
      >
        {{ segment.text }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// Props
const props = defineProps({
  oldName: {
    type: String,
    required: true
  },
  newName: {
    type: String,
    required: true
  }
})

// 简单的 Diff 算法 - 逐字符比较
const diffSegments = computed(() => {
  const old = props.oldName
  const newStr = props.newName
  const segments = []

  // 使用最长公共子序列(LCS)算法进行 diff
  const lcs = getLCS(old, newStr)

  let oldIndex = 0
  let newIndex = 0
  let lcsIndex = 0

  while (oldIndex < old.length || newIndex < newStr.length) {
    // 如果当前字符在 LCS 中,说明是相同的
    if (lcsIndex < lcs.length && oldIndex < old.length && old[oldIndex] === lcs[lcsIndex]) {
      // 找到相同的连续段
      let sameText = ''
      while (lcsIndex < lcs.length && oldIndex < old.length && old[oldIndex] === lcs[lcsIndex]) {
        sameText += old[oldIndex]
        oldIndex++
        newIndex++
        lcsIndex++
      }
      segments.push({ type: 'same', text: sameText })
    } else if (oldIndex < old.length && (lcsIndex >= lcs.length || old[oldIndex] !== lcs[lcsIndex])) {
      // 旧文件中被删除的部分
      let removedText = ''
      while (oldIndex < old.length && (lcsIndex >= lcs.length || old[oldIndex] !== lcs[lcsIndex])) {
        removedText += old[oldIndex]
        oldIndex++
      }
      if (removedText) {
        segments.push({ type: 'removed', text: removedText })
      }
    } else if (newIndex < newStr.length) {
      // 新文件中添加的部分
      let addedText = ''
      while (newIndex < newStr.length && (lcsIndex >= lcs.length || newStr[newIndex] !== lcs[lcsIndex])) {
        addedText += newStr[newIndex]
        newIndex++
      }
      if (addedText) {
        segments.push({ type: 'added', text: addedText })
      }
    }
  }

  return segments
})

// 最长公共子序列算法 (LCS)
function getLCS(str1, str2) {
  const m = str1.length
  const n = str2.length
  const dp = Array(m + 1).fill(null).map(() => Array(n + 1).fill(0))

  // 构建 DP 表
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (str1[i - 1] === str2[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
      }
    }
  }

  // 回溯找到 LCS 字符串
  let lcs = ''
  let i = m, j = n
  while (i > 0 && j > 0) {
    if (str1[i - 1] === str2[j - 1]) {
      lcs = str1[i - 1] + lcs
      i--
      j--
    } else if (dp[i - 1][j] > dp[i][j - 1]) {
      i--
    } else {
      j--
    }
  }

  return lcs
}
</script>

<style scoped>
.filename-diff {
  display: inline-flex;
  flex-wrap: wrap;
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.6;
}

.diff-segment {
  display: inline;
}

/* 相同部分：正常文本色 */
.diff-same {
  color: var(--text-main);
  font-weight: 600;
}

/* 删除部分：使用设计 token */
.diff-removed {
  color: var(--diff-del-text);
  text-decoration: line-through;
  background-color: var(--diff-del-bg);
  padding: 2px 4px;
  border-radius: 3px;
  opacity: 0.8;
}

/* 新增部分：使用设计 token */
.diff-added {
  color: var(--diff-add-text);
  font-weight: 600;
  background-color: var(--diff-add-bg);
  padding: 2px 4px;
  border-radius: 3px;
}
</style>
