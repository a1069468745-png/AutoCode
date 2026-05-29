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
const status = ref('')
const resultMessage = ref('')

const form = reactive({
  intent: 'CODE_LOCATE' as QueryIntent,
  question: '',
})

const intentOptions = [
  { value: 'CODE_LOCATE', label: 'Code locate' },
  { value: 'HISTORY_TRACE', label: 'History trace' },
  { value: 'DOCUMENT_TRACE', label: 'Document trace' },
  { value: 'CALL_RELATION', label: 'Impact analysis' },
  { value: 'REQUIREMENT_ANALYSIS', label: 'Requirement traceability' },
]

const hasProject = computed(() => Boolean(appStore.activeProjectId))

async function submitQuery() {
  if (!hasProject.value) {
    ElMessage.warning('Please set an active project first.')
    return
  }
  if (!form.question.trim()) {
    ElMessage.warning('Please enter a natural-language question.')
    return
  }

  loading.value = true
  try {
    const response = await runQuery(form.intent, {
      projectId: Number(appStore.activeProjectId),
      queryText: form.question.trim(),
      preferredIntent: form.intent,
      options: {},
    })
    hits.value = response.result.hits
    resolvedIntent.value = response.intent
    cacheHit.value = response.cacheHit
    status.value = response.result.status
    resultMessage.value = response.result.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="query-grid">
    <article class="panel">
      <h2>Query Request</h2>
      <el-form label-position="top">
        <el-form-item label="Intent">
          <el-select v-model="form.intent" class="full-width">
            <el-option v-for="item in intentOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="Natural-language question">
          <el-input
            v-model="form.question"
            type="textarea"
            :rows="5"
            placeholder="Where is the refund entry point in the payment module?"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="submitQuery">Run query</el-button>
      </el-form>
      <p class="tips">Active project: {{ appStore.activeProjectName }}</p>
    </article>

    <article class="panel">
      <h2>Query Result</h2>
      <p class="meta" v-if="resolvedIntent">
        Routed intent {{ resolvedIntent }}, status {{ status }}, cache hit {{ cacheHit ? 'yes' : 'no' }}
      </p>
      <p class="meta" v-if="resultMessage">{{ resultMessage }}</p>
      <el-empty v-if="!hits.length && !loading" description="No results yet." />
      <el-table v-else :data="hits" v-loading="loading">
        <el-table-column prop="sourceType" label="Source" width="140" />
        <el-table-column prop="title" label="Title" min-width="220" />
        <el-table-column prop="excerpt" label="Excerpt" min-width="300" show-overflow-tooltip />
        <el-table-column prop="score" label="Score" width="100" />
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
