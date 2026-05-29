<script setup lang="ts">
import { DataLine, Document, FolderOpened, Link } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type MenuItem = {
  key: string
  label: string
  icon: unknown
  routeName?: string
}

const menuItems: MenuItem[] = [
  { key: 'overview', label: '平台概览', icon: DataLine, routeName: 'overview' },
  { key: 'projects', label: '项目管理', icon: FolderOpened, routeName: 'projects' },
  { key: 'queries', label: '查询联调', icon: Link, routeName: 'queries' },
  { key: 'knowledge', label: '知识回溯', icon: Document },
]

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => {
  const active = menuItems.find((item) => item.routeName === route.name)
  return active?.key ?? 'overview'
})

function handleMenuSelect(key: string) {
  const target = menuItems.find((item) => item.key === key)
  if (!target?.routeName) {
    return
  }
  router.push({ name: target.routeName })
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside class="shell-aside" width="240px">
      <div class="brand-block">
        <p class="brand-mark">AutoCode</p>
        <h1>Web Console</h1>
        <span>一期联调</span>
      </div>

      <el-menu class="shell-menu" :default-active="activeMenu" @select="handleMenuSelect">
        <el-menu-item v-for="item in menuItems" :key="item.key" :index="item.key">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="shell-header">
        <div>
          <p class="header-label">当前阶段</p>
          <strong>项目与查询联调推进</strong>
        </div>
        <el-tag type="warning" effect="dark">IT-08 / IT-09 / IT-10</el-tag>
      </el-header>

      <el-main class="shell-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.shell-aside {
  color: #f9fafb;
  padding: 28px 20px 20px;
  background:
    linear-gradient(180deg, rgba(16, 24, 40, 0.95), rgba(31, 41, 55, 0.98)),
    radial-gradient(circle at top, rgba(251, 191, 36, 0.25), transparent 32%);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-block {
  margin-bottom: 24px;
}

.brand-mark {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: #fbbf24;
}

.brand-block h1 {
  margin: 0;
  font-size: 30px;
}

.brand-block span {
  display: inline-block;
  margin-top: 8px;
  color: rgba(249, 250, 251, 0.72);
}

.shell-menu {
  border-right: none;
  background: transparent;
}

.shell-menu :deep(.el-menu-item) {
  margin-bottom: 6px;
  border-radius: 12px;
  color: rgba(249, 250, 251, 0.78);
}

.shell-menu :deep(.el-menu-item.is-active) {
  color: #fff7ed;
  background: rgba(245, 158, 11, 0.16);
}

.shell-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: auto;
  padding: 24px 28px 0;
}

.header-label {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #9a3412;
}

.shell-main {
  padding: 28px;
}

@media (max-width: 960px) {
  .app-shell {
    flex-direction: column;
  }

  .shell-aside {
    width: 100% !important;
  }

  .shell-header,
  .shell-main {
    padding-inline: 20px;
  }
}
</style>
