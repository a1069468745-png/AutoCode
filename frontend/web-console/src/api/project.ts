import { http } from '@/api/http'

export type ProjectSummary = {
  id: number
  name: string
  repoUrl: string
  defaultBranch: string
  status: string
}

export type CreateProjectPayload = {
  name: string
  repoUrl: string
  defaultBranch: string
  languageStack?: string | null
  docRepoPath?: string | null
}

export type ProjectIndexSyncPayload = {
  workspaceRoot?: string | null
  maxCommits?: number | null
}

export type ProjectIndexSyncResponse = {
  projectId: number
  status: string
  workspaceRoot: string
  symbolCount: number
  edgeCount: number
  commitCount: number
  documentCount: number
  requirementCount: number
  linkCount: number
}

export async function listProjects() {
  const response = await http.get<ProjectSummary[]>('/projects')
  return response.data
}

export async function createProject(payload: CreateProjectPayload) {
  const response = await http.post<{ id: number; name: string; status: string }>('/projects', payload)
  return response.data
}

export async function syncProjectIndexes(projectId: number, payload: ProjectIndexSyncPayload = {}) {
  const response = await http.post<ProjectIndexSyncResponse>(`/projects/${projectId}/sync-indexes`, payload)
  return response.data
}
