<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { createProject, listProjects, syncProjectIndexes, type ProjectSummary } from '@/api/project'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(false)
const syncingProjectId = ref<number | null>(null)
const projects = ref<ProjectSummary[]>([])

const form = reactive({
  name: '',
  repoUrl: '',
  defaultBranch: 'main',
  docRepoPath: '',
  languageStack: 'java,spring-boot,vue',
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
  if (!form.name || !form.repoUrl || !form.defaultBranch) {
    ElMessage.warning('Please complete the project information first.')
    return
  }

  loading.value = true
  try {
    await createProject({
      name: form.name,
      repoUrl: form.repoUrl,
      defaultBranch: form.defaultBranch,
      docRepoPath: form.docRepoPath || null,
      languageStack: form.languageStack || null,
    })

    ElMessage.success('Project created successfully.')
    await refreshProjects()
  } finally {
    loading.value = false
  }
}

function activateProject(project: ProjectSummary) {
  // Persist the selected project so query requests keep the same scope header.
  appStore.setActiveProject(String(project.id), project.name)
  ElMessage.success(`Active project set to ${project.name}.`)
}

async function syncIndexes(project: ProjectSummary) {
  syncingProjectId.value = project.id
  try {
    const result = await syncProjectIndexes(project.id, {})
    ElMessage.success(
      `Synced ${result.symbolCount} symbols, ${result.commitCount} commits, and ${result.documentCount} documents.`,
    )
    await refreshProjects()
  } finally {
    syncingProjectId.value = null
  }
}

onMounted(async () => {
  await refreshProjects()
})
</script>

<template>
  <section class="projects-grid">
    <article class="panel">
      <h2>Create Project</h2>
      <el-form label-position="top">
        <el-form-item label="Project name">
          <el-input v-model="form.name" placeholder="AutoCode" />
        </el-form-item>
        <el-form-item label="Repository URL">
          <el-input v-model="form.repoUrl" placeholder="https://git.example.com/team/repo.git" />
        </el-form-item>
        <el-form-item label="Default branch">
          <el-input v-model="form.defaultBranch" placeholder="main" />
        </el-form-item>
        <el-form-item label="Document repository path">
          <el-input v-model="form.docRepoPath" placeholder="docs/specs" />
        </el-form-item>
        <el-form-item label="Language stack">
          <el-input v-model="form.languageStack" placeholder="java,spring-boot,vue" />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="submitCreateProject">Create and refresh</el-button>
      </el-form>
    </article>

    <article class="panel">
      <h2>Project List</h2>
      <el-table :data="projects" v-loading="loading" empty-text="No project records yet.">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="Project" min-width="180" />
        <el-table-column prop="defaultBranch" label="Branch" width="120" />
        <el-table-column prop="status" label="Status" width="120" />
        <el-table-column label="Action" width="220">
          <template #default="scope">
            <el-button link type="primary" @click="activateProject(scope.row)">Set active</el-button>
            <el-button
              link
              type="success"
              :loading="syncingProjectId === scope.row.id"
              @click="syncIndexes(scope.row)"
            >
              Sync indexes
            </el-button>
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
