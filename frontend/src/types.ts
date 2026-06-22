export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface UserProfile {
  id: number
  phone: string
  displayName: string
  avatarUrl?: string
  role: 'USER' | 'ADMIN'
  active: boolean
}

export interface AuthResponse {
  token: string
  user: UserProfile
}

export interface QuestionOption {
  id: number
  label: string
  content: string
  weights: Record<string, number>
}

export interface Question {
  id: number
  type: string
  content: string
  active: boolean
  options: QuestionOption[]
}

export interface TestResult {
  id: number
  type: string
  scores: Record<string, number>
  createdAt: string
}

export interface RadarIndicator {
  name: string
  max: number
}

export interface Report {
  user: UserProfile
  scores: Record<string, number>
  indicators: RadarIndicator[]
  radarValues: number[]
  interpretations: string[]
  suggestions: string[]
  generatedAt: string
}

export interface ReportSnapshot {
  id: number
  report: Report
  summary: string
  createdAt: string
}

export interface Recommendation {
  id: number
  scene: string
  title: string
  description: string
  tags: string[]
  score: number
  baseScore?: number
  active?: boolean
}

export interface AiRecommendationItem {
  name: string
  category?: string
  address?: string
  distanceMeters?: number
  rating?: number
  priceLevel?: string
  matchScore: number
  reason?: string
  tags?: string[]
  mapUrl?: string
}

export interface AiRecommendationResponse {
  recordId: number
  scene: string
  source: string
  degraded: boolean
  city?: string
  generatedAt: string
  items: AiRecommendationItem[]
}
export interface MatchReport {
  id: number
  owner: UserProfile
  target: UserProfile
  score: number
  summary: string
  advantages: string[]
  warnings: string[]
  advice: string[]
  ownerScores: Record<string, number>
  targetScores: Record<string, number>
  createdAt: string
}

export interface MatchInvite {
  code: string
  createdAt: string
  status: 'ACTIVE' | 'USED' | 'REVOKED'
  expiresAt: string
}

export interface MatchByInviteRequest {
  inviteCode: string
}

export interface UserFeedback {
  id: number
  itemTitle: string
  scene: string
  rating: string
  comment?: string
  createdAt: string
}

export interface AdminStats {
  users: number
  questions: number
  recommendations: number
  feedbacks: number
  matches: number
}

export interface ShareLinkSummary {
  id: number
  token: string
  url: string
  active: boolean
  createdAt: string
  expiresAt?: string
  revokedAt?: string
}

export interface AdminDashboard {
  stats: AdminStats
  testsByType: Record<string, number>
  feedbackByRating: Record<string, number>
  recommendationsByScene: Record<string, number>
  activeShares: number
}

export interface AdminUser {
  id: number
  phone: string
  displayName: string
  avatarUrl?: string
  role: 'USER' | 'ADMIN'
  active: boolean
  failedLoginAttempts: number
  lockedUntil?: string
  lastLoginAt?: string
  createdAt: string
}

export interface RecommendationRule {
  id: number
  tag: string
  label: string
  weight: number
  active: boolean
}
