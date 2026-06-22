import axios from 'axios'
import type {
  AdminDashboard,
  AdminStats,
  AdminUser,
  ApiResponse,
  AiRecommendationResponse,
  AuthResponse,
  MatchInvite,
  MatchReport,
  Question,
  Recommendation,
  RecommendationRule,
  Report,
  ReportSnapshot,
  ShareLinkSummary,
  TestResult,
  UserFeedback,
  UserProfile
} from './types'

export const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('radar_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (typeof body?.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常，请稍后再试'
    return Promise.reject(new Error(message))
  }
)

const dataOf = <T>(promise: Promise<{ data: ApiResponse<T> }>) => promise.then((res) => res.data.data)

export const authApi = {
  register: (payload: { phone: string; password: string; displayName: string }) =>
    dataOf<AuthResponse>(api.post('/auth/register', payload)),
  login: (payload: { phone: string; password: string }) => dataOf<AuthResponse>(api.post('/auth/login', payload)),
  me: () => dataOf<UserProfile>(api.get('/users/me')),
  updateMe: (payload: { displayName?: string; avatarUrl?: string }) => dataOf<UserProfile>(api.put('/users/me', payload))
}

export const testApi = {
  questions: (type: string) => dataOf<Question[]>(api.get('/questions', { params: { type } })),
  submit: (payload: { type: string; answers: { questionId: number; optionIds: number[] }[] }) =>
    dataOf<TestResult>(api.post('/tests/submit', payload)),
  history: () => dataOf<TestResult[]>(api.get('/tests/history'))
}

export const reportApi = {
  me: () => dataOf<Report>(api.get('/reports/me')),
  history: () => dataOf<ReportSnapshot[]>(api.get('/reports/history')),
  get: (id: number) => dataOf<ReportSnapshot>(api.get(`/reports/${id}`)),
  share: () => dataOf<{ token: string; url: string; report: Report }>(api.post('/shares/report')),
  shares: () => dataOf<ShareLinkSummary[]>(api.get('/shares')),
  revokeShare: (id: number) => dataOf<void>(api.delete(`/shares/${id}`)),
  byToken: (token: string) => dataOf<Report>(api.get(`/shares/${token}`))
}

export const recommendationApi = {
  list: (scene: string) => dataOf<Recommendation[]>(api.get('/recommendations', { params: { scene } })),
  feedback: (id: number, payload: { rating: string; comment?: string }) =>
    dataOf<void>(api.post(`/recommendations/${id}/feedback`, payload)),
  myFeedback: () => dataOf<UserFeedback[]>(api.get('/recommendations/feedback/me'))
}


export const aiRecommendationApi = {
  recommend: (params: { scene?: string; lat?: number; lng?: number; city?: string; limit?: number }) =>
    dataOf<AiRecommendationResponse>(api.get('/recommendations/ai', { params })),
  feedback: (recordId: number, payload: { rating: string; comment?: string; itemName?: string }) =>
    dataOf<void>(api.post(`/recommendations/ai/${recordId}/feedback`, payload)),
  history: () => dataOf<unknown[]>(api.get('/recommendations/ai/history'))
}
export const matchApi = {
  create: (friendPhone: string) => dataOf<MatchReport>(api.post('/matches', { friendPhone })),
  list: () => dataOf<MatchReport[]>(api.get('/matches')),
  get: (id: number) => dataOf<MatchReport>(api.get(`/matches/${id}`)),
  createInvite: () => dataOf<MatchInvite>(api.post('/matches/invite')),
  listInvites: () => dataOf<MatchInvite[]>(api.get('/matches/invites')),
  matchByInvite: (inviteCode: string) => dataOf<MatchReport>(api.post('/matches/by-invite', { inviteCode }))
}

export const adminApi = {
  stats: () => dataOf<AdminStats>(api.get('/admin/stats')),
  dashboard: () => dataOf<AdminDashboard>(api.get('/admin/dashboard')),
  questions: () => dataOf<Question[]>(api.get('/admin/questions')),
  feedback: () => dataOf<unknown[]>(api.get('/admin/feedback')),
  logs: () => dataOf<unknown[]>(api.get('/admin/logs')),
  users: () => dataOf<AdminUser[]>(api.get('/admin/users')),
  updateUser: (id: number, payload: { active?: boolean; role?: string }) =>
    dataOf<AdminUser>(api.put(`/admin/users/${id}`, payload)),
  recommendationItems: () => dataOf<Recommendation[]>(api.get('/admin/recommendation-items')),
  createQuestion: (payload: unknown) => dataOf<Question>(api.post('/admin/questions', payload)),
  updateQuestion: (id: number, payload: unknown) => dataOf<Question>(api.put(`/admin/questions/${id}`, payload)),
  deleteQuestion: (id: number) => dataOf<void>(api.delete(`/admin/questions/${id}`)),
  createRecommendation: (payload: unknown) => dataOf<Recommendation>(api.post('/admin/recommendation-items', payload)),
  updateRecommendation: (id: number, payload: unknown) => dataOf<Recommendation>(api.put(`/admin/recommendation-items/${id}`, payload)),
  deleteRecommendation: (id: number) => dataOf<void>(api.delete(`/admin/recommendation-items/${id}`)),
  recommendationRules: () => dataOf<RecommendationRule[]>(api.get('/admin/recommendation-rules')),
  createRecommendationRule: (payload: unknown) => dataOf<RecommendationRule>(api.post('/admin/recommendation-rules', payload)),
  updateRecommendationRule: (id: number, payload: unknown) =>
    dataOf<RecommendationRule>(api.put(`/admin/recommendation-rules/${id}`, payload)),
  deleteRecommendationRule: (id: number) => dataOf<void>(api.delete(`/admin/recommendation-rules/${id}`))
}
