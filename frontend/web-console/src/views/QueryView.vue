<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'

import { runQuery, type QueryHit, type QueryIntent } from '@/api/query'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(false)
const hits = ref<QueryHit[]>([])
const resolvedIntent = ref('')
const cacheHit = ref(false)
const confidence = ref(0)

const form = reactive({
  intent: 'CODE_LOCATE' as QueryIntent,
  question: '',
})

const intentOptions = [
  { value: 'CODE_LOCATE', label: '代码定位' },
  { value: 'HISTORY_TRACE', label: '历史追溯' },
  { value: 'DOCUMENT_TRACE', label: '文档回溯' },
  { value: 'CALL_RELATION', label: '影响分析' },
  { value: 'REQUIREMENT_ANALYSIS', label: '需求追溯' },
]

const hasProject = computed(() => Boolean(appStore.activeProjectId))

async function submitQuery() {
  if (!hasProject.value) {
    ElMessage.warning('请先在项目管理页设置当前项目')
    return
  }
  if (!form.question.trim()) {
    ElMessage.warning('请输入查询问题')
    return
  }

  loading.value = true
  try {
    const response = await runQuery(form.intent, { question: form.question.trim() })
    hits.value = response.hits
    resolvedIntent.value = response.intent
    cacheHit.value = response.cacheHit
    confidence.value = response.confidence
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="query-grid">
    <article class="panel">
      <h2>查询请求</h2>
      <el-form label-position="top">
        <el-form-item label="查询类型">
          <el-select v-model="form.intent" class="full-width">
            <el-option v-for="item in intentOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="自然语言问题">
          <el-input v-model="form.question" type="textarea" :rows="5" placeholder="例如：payment 模块退款入口函数在哪里？" />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="submitQuery">执行查询</el-button>
      </el-form>
      <p class="tips">当前项目：{{ appStore.activeProjectName }}</p>
    </article>

    <article class="panel">
      <h2>查询结果</h2>
      <p class="meta" v-if="resolvedIntent">
        路由意图 {{ resolvedIntent }}，置信度 {{ confidence.toFixed(2) }}，缓存命中 {{ cacheHit ? '是' : '否' }}
      </p>
      <el-empty v-if="!hits.length && !loading" description="暂无结果" />
      <el-table v-else :data="hits" v-loading="loading">
        <el-table-column prop="sourceType" label="来源" width="140" />
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="excerpt" label="摘要" min-width="300" show-overflow-tooltip />
        <el-table-column prop="score" label="分数" width="100" />
      </el-table>
    </article>
  </section>
</template>

<style scoped>
.query-grid {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 20px;
}

.panel {
  border: 1px solid rgba(180, 83, 9, 0.14);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  padding: 20px;
}

.panel h2 {
  margin: 0 0 16px;
}

.full-width {
  width: 100%;
}

.tips,
.meta {
  margin-top: 12px;
  color: #6b7280;
}

@media (max-width: 960px) {
  .query-grid {
    grid-template-columns: 1fr;
  }
}
</style>
