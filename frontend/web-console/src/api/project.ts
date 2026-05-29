import { http } from '@/api/http'

export type ProjectSummary = {
  id: number
  name: string
  repositoryUrl: string
  defaultBranch: string
  techStack: string[]
  status: string
}

export type CreateProjectPayload = {
  name: string
  repositoryUrl: string
  defaultBranch: string
  docRepoPath?: string | null
  techStack?: string[]
}

export async function listProjects() {
  const response = await http.get<ProjectSummary[]>('/projects')
  return response.data
}

export async function createProject(payload: CreateProjectPayload) {
  const response = await http.post<{ id: number; name: string; status: string }>('/projects', payload)
  return response.data
}
