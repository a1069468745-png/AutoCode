import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', () => {
  const activeProjectId = ref<string | null>(null)
  const activeProjectName = ref('未选择项目')
  const environment = ref(import.meta.env.MODE)

  const headerProjectId = computed(() => activeProjectId.value ?? '')

  function setActiveProject(projectId: string | null, projectName: string) {
    activeProjectId.value = projectId
    activeProjectName.value = projectName

    if (projectId) {
      window.sessionStorage.setItem('autocode.projectId', projectId)
      return
    }

    window.sessionStorage.removeItem('autocode.projectId')
  }

  return {
    activeProjectId,
    activeProjectName,
    environment,
    headerProjectId,
    setActiveProject,
  }
})
