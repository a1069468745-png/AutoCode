<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const { activeProjectName, environment } = storeToRefs(appStore)
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'
</script>

<template>
  <section class="overview-grid">
    <article class="hero-panel">
      <p class="eyebrow">阶段目标</p>
      <h2>IT-08 / IT-09 / IT-10 联调推进中</h2>
      <p class="hero-copy">
        当前控制台已接入项目管理与查询联调页面，可直接创建项目、切换当前项目并发起多类型查询。
      </p>
      <div class="hero-tags">
        <el-tag effect="plain">Vue 3</el-tag>
        <el-tag effect="plain">TypeScript</el-tag>
        <el-tag effect="plain">Element Plus</el-tag>
        <el-tag effect="plain">Pinia</el-tag>
        <el-tag effect="plain">Axios</el-tag>
      </div>
    </article>

    <article class="status-panel">
      <div class="status-item">
        <span>当前项目</span>
        <strong>{{ activeProjectName }}</strong>
      </div>
      <div class="status-item">
        <span>运行环境</span>
        <strong>{{ environment }}</strong>
      </div>
      <div class="status-item">
        <span>API Base</span>
        <strong>{{ apiBaseUrl }}</strong>
      </div>
    </article>
  </section>
</template>

<style scoped>
.overview-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.hero-panel,
.status-panel {
  border: 1px solid rgba(180, 83, 9, 0.14);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 18px 40px rgba(120, 53, 15, 0.08);
  backdrop-filter: blur(8px);
}

.hero-panel {
  padding: 28px;
}

.eyebrow {
  margin: 0 0 12px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #b45309;
}

.hero-panel h2 {
  margin: 0;
  font-size: 36px;
  line-height: 1.1;
  color: #111827;
}

.hero-copy {
  max-width: 640px;
  margin: 16px 0 0;
  font-size: 16px;
  color: #4b5563;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.status-panel {
  padding: 24px;
}

.status-item + .status-item {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(180, 83, 9, 0.12);
}

.status-item span {
  color: #9a3412;
}

.status-item strong {
  display: block;
  margin-top: 6px;
  font-size: 18px;
  color: #1f2937;
}

@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .hero-panel,
  .status-panel {
    padding: 20px;
  }

  .hero-panel h2 {
    font-size: 28px;
  }
}
</style>
