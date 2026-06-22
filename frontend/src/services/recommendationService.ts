import { aiRecommendationApi, recommendationApi } from '../api'
import type { AiRecommendationResponse, Recommendation, UserFeedback } from '../types'

export async function listRecommendations(scene: string) {
  return recommendationApi.list(scene) as Promise<Recommendation[]>
}

export async function submitFeedback(id: number, rating: string, comment?: string) {
  return recommendationApi.feedback(id, { rating, comment })
}

export async function getMyFeedback() {
  return recommendationApi.myFeedback() as Promise<UserFeedback[]>
}
export async function listAiRecommendations(params: { scene?: string; lat?: number; lng?: number; city?: string; limit?: number }) {
  return aiRecommendationApi.recommend(params) as Promise<AiRecommendationResponse>
}

export async function submitAiFeedback(recordId: number, payload: { rating: string; comment?: string; itemName?: string }) {
  return aiRecommendationApi.feedback(recordId, payload)
}
