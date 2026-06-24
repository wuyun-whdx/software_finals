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

export interface LocationRecommendation extends Recommendation {
  address?: string
  aiReason?: string
  source?: 'ai' | 'general'
}

export interface SimpleRegion {
  id: number
  name: string
}

export interface RegionInfo {
  province: string
  city: string
  district?: string
}

export interface RegionRecord {
  province: string
  city: string
  district: string
  isCurrent: boolean
  createdAt: string
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

// Community
export interface PostResponse {
  id: number
  author: UserProfile
  content: string
  images: string
  domainTag: string
  styleTags: string[]
  aiVector: Record<string, number>
  aiReviewStatus: string
  likeCount: number
  favoriteCount: number
  commentCount: number
  viewCount: number
  compatibility: number
  showCompatibility: boolean
  createdAt: string
  updatedAt: string
}

export interface PostListResponse {
  items: PostResponse[]
  total: number
}

export interface CommentResponse {
  id: number
  content: string
  user: UserProfile
  createdAt: string
}

export interface CreatePostRequest {
  content: string
  domainTag: string
  styleTags?: string[]
}

export interface CreateCommentRequest {
  content: string
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

// === Friend & Chat ===
export interface FriendInvite {
  code: string
  createdAt: string
  status: 'ACTIVE' | 'USED' | 'REVOKED'
  expiresAt: string
}

export interface FriendRequestItem {
  id: number
  fromUser: UserProfile
  toUser: UserProfile
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'BLOCKED'
  message: string
  createdAt: string
}

export interface OpenMatchStatus {
  enabled: boolean
  message: string
}

export interface OpenMatchRecommendation {
  user: UserProfile
  score: number
  topDimensions: string[]
}

export interface ChatMessage {
  id: number
  sender: UserProfile
  content: string
  read: boolean
  createdAt: string
}

export interface ChatConversation {
  friend: UserProfile
  lastMessage: string
  lastMessageTime: string
  unreadCount: number
}

