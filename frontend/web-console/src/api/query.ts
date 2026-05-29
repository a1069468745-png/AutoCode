import { http } from '@/api/http'

export type QueryIntent = 'CODE_LOCATE' | 'HISTORY_TRACE' | 'DOCUMENT_TRACE' | 'CALL_RELATION' | 'REQUIREMENT_ANALYSIS'

export type QueryRequest = {
  projectId: number
  queryText: string
  preferredIntent?: QueryIntent
  options?: Record<string, unknown>
}

export type QueryHit = {
  sourceType: string
  sourceId: string
  title: string
  excerpt: string
  score: number
  metadata: Record<string, string>
}

export type QueryResponse = {
  intent: string
  result: {
    status: string
    hits: QueryHit[]
    message: string
  }
  cacheHit: boolean
  context: Record<string, unknown>
}

type ApiEnvelope<T> = {
  code: string
  message: string
  data: T
}

const endpointMap: Record<QueryIntent, string> = {
  CODE_LOCATE: '/query/code',
  HISTORY_TRACE: '/query/history',
  DOCUMENT_TRACE: '/query/knowledge',
  CALL_RELATION: '/query/impact',
  REQUIREMENT_ANALYSIS: '/query/traceability',
}

export async function runQuery(intent: QueryIntent, request: QueryRequest) {
  // Keep endpoint selection explicit so frontend intent and backend routing stay aligned.
  const endpoint = endpointMap[intent]
  const response = await http.post<ApiEnvelope<QueryResponse>>(endpoint, request)
  return response.data.data
}
