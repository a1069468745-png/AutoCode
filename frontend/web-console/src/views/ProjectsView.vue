<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { createProject, listProjects, type ProjectSummary } from '@/api/project'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(false)
const projects = ref<ProjectSummary[]>([])

const form = reactive({
  name: '',
  repositoryUrl: '',
  defaultBranch: 'main',
  docRepoPath: '',
  techStackText: 'java,spring-boot,vue',
})

async function refreshProjects() {
  loading.value = true
  try {
    projects.value = await listProjects()
  } finally {
    loading.value = false
  }
}

async function submitCreateProject() {
  if (!form.name || !form.repositoryUrl || !form.defaultBranch) {
    ElMessage.warning('请先填写完整项目信息')
    return
  }

  loading.value = true
  try {
    // Normalize comma-separated stack input into backend expected array.
    const techStack = form.techStackText
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)

    await createProject({
      name: form.name,
      repositoryUrl: form.repositoryUrl,
      defaultBranch: form.defaultBranch,
      docRepoPath: form.docRepoPath || null,
      techStack,
    })

    ElMessage.success('项目已创建')
    await refreshProjects()
  } finally {
    loading.value = false
  }
}

function activateProject(project: ProjectSummary) {
  // Persist selected project id for gateway X-Project-Id enforcement.
  appStore.setActiveProject(String(project.id), project.name)
  ElMessage.success(`已切换到项目 ${project.name}`)
}

onMounted(async () => {
  await refreshProjects()
})
</script>

<template>
  <section class="projects-grid">
    <article class="panel">
      <h2>创建项目</h2>
      <el-form label-position="top">
        <el-form-item label="项目名">
          <el-input v-model="form.name" placeholder="例如：AutoCode" />
        </el-form-item>
        <el-form-item label="仓库地址">
          <el-input v-model="form.repositoryUrl" placeholder="https://git.example.com/team/repo.git" />
        </el-form-item>
        <el-form-item label="默认分支">
          <el-input v-model="form.defaultBranch" placeholder="main" />
        </el-form-item>
        <el-form-item label="文档仓路径">
          <el-input v-model="form.docRepoPath" placeholder="docs/specs" />
        </el-form-item>
        <el-form-item label="技术栈（逗号分隔）">
          <el-input v-model="form.techStackText" />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="submitCreateProject">创建并刷新</el-button>
      </el-form>
    </article>

    <article class="panel">
      <h2>项目列表</h2>
      <el-table :data="projects" v-loading="loading" empty-text="暂无项目">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="项目名" min-width="180" />
        <el-table-column prop="defaultBranch" label="分支" width="120" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button link type="primary" @click="activateProject(scope.row)">设为当前</el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>
  </section>
</template>

<style scoped>
.projects-grid {
  display: grid;
  grid-template-columns: 1fr 1.3fr;
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

@media (max-width: 960px) {
  .projects-grid {
    grid-template-columns: 1fr;
  }
}
</style>
