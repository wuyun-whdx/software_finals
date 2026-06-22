/**
 * Mock 数据层 — 无需后端即可预览所有 UI 界面
 *
 * 使用方式：在 main.ts 中 `import './mock'`
 * 移除方式：注释掉 main.ts 中的 import，恢复真实 API
 *
 * 覆盖范围：
 *  - 登录/注册（任意手机号+密码直接通过）
 *  - 工作台 Dashboard（模拟指标、快照、推荐）
 *  - 测试页（性格/饮食/旅游 三类题目）
 *  - 报告页（ECharts 雷达图 + 文字解读 + 生活建议 + 历史快照 + 分享管理）
 *  - 推荐页（餐饮/旅行/穿搭/生涯 四大场景）
 *  - 适配页（双人兼容性匹配）
 *  - 个人中心（资料编辑 + 历史记录）
 *  - 分享页（免登录只读报告）
 */

import { api } from './api'
import type { ApiResponse } from './types'

// 模拟网络延迟（300-600ms），保留真实体感
function delay(): Promise<void> {
  return new Promise((r) => setTimeout(r, 300 + Math.random() * 300))
}

// ============================================================
// Mock 数据
// ============================================================

const MOCK_USER = {
  id: 1,
  phone: '139****0001',
  displayName: '晨间探索者',
  avatarUrl: '',
  role: 'USER' as const,
  active: true,
}

const MOCK_ADMIN = {
  id: 0,
  phone: '138****0000',
  displayName: '管理员',
  avatarUrl: '',
  role: 'ADMIN' as const,
  active: true,
}

const PERSONALITY_QUESTIONS = [
  {
    id: 1,
    type: 'PERSONALITY',
    content: '周末你更倾向于如何度过？',
    active: true,
    options: [
      { id: 1, label: 'A', content: '参加聚会，认识新朋友', weights: { EXTRAVERSION: 5, OPENNESS: 3 } },
      { id: 2, label: 'B', content: '在家看书或看电影', weights: { INTROVERSION: 4, OPENNESS: 3 } },
      { id: 3, label: 'C', content: '去户外徒步或骑行', weights: { OPENNESS: 5, EXTRAVERSION: 3 } },
      { id: 4, label: 'D', content: '整理房间，规划下周安排', weights: { CONSCIENTIOUSNESS: 5, NEUROTICISM: -2 } },
    ],
  },
  {
    id: 2,
    type: 'PERSONALITY',
    content: '面对一个重要的决定，你通常会？',
    active: true,
    options: [
      { id: 5, label: 'A', content: '跟着直觉走', weights: { OPENNESS: 4, NEUROTICISM: 2 } },
      { id: 6, label: 'B', content: '列出优缺点逐条分析', weights: { CONSCIENTIOUSNESS: 5, NEUROTICISM: -1 } },
      { id: 7, label: 'C', content: '咨询朋友或家人的意见', weights: { AGREEABLENESS: 4, EXTRAVERSION: 3 } },
      { id: 8, label: 'D', content: '先放一放，等感觉对了再决定', weights: { NEUROTICISM: 3, OPENNESS: 2 } },
    ],
  },
  {
    id: 3,
    type: 'PERSONALITY',
    content: '在团队项目中，你更倾向于扮演什么角色？',
    active: true,
    options: [
      { id: 9, label: 'A', content: '提出创意和方向的 leader', weights: { EXTRAVERSION: 5, OPENNESS: 4 } },
      { id: 10, label: 'B', content: '确保细节不出错的执行者', weights: { CONSCIENTIOUSNESS: 5, NEUROTICISM: 1 } },
      { id: 11, label: 'C', content: '协调关系、化解矛盾的润滑剂', weights: { AGREEABLENESS: 5, EXTRAVERSION: 2 } },
      { id: 12, label: 'D', content: '独立完成分配给我的那部分', weights: { CONSCIENTIOUSNESS: 3, AGREEABLENESS: 2 } },
    ],
  },
  {
    id: 4,
    type: 'PERSONALITY',
    content: '遇到压力时，你通常会？',
    active: true,
    options: [
      { id: 13, label: 'A', content: '找朋友倾诉或出去转转', weights: { EXTRAVERSION: 4, AGREEABLENESS: 3 } },
      { id: 14, label: 'B', content: '制定详细计划来解决问题', weights: { CONSCIENTIOUSNESS: 5, NEUROTICISM: -3 } },
      { id: 15, label: 'C', content: '深呼吸、冥想，让自己冷静', weights: { NEUROTICISM: -2, OPENNESS: 2 } },
      { id: 16, label: 'D', content: '暂时转移注意力到其他事情上', weights: { NEUROTICISM: 2, OPENNESS: 3 } },
    ],
  },
]

const FOOD_QUESTIONS = [
  {
    id: 5,
    type: 'FOOD',
    content: '选择餐厅时，你最看重什么？',
    active: true,
    options: [
      { id: 17, label: 'A', content: '口味地道、评价高', weights: { food_adventure: 4, food_tradition: 3 } },
      { id: 18, label: 'B', content: '环境优雅、适合拍照', weights: { food_aesthetic: 5, food_social: 3 } },
      { id: 19, label: 'C', content: '价格实惠、性价比高', weights: { food_practical: 5, food_tradition: 2 } },
      { id: 20, label: 'D', content: '新奇有趣、从未尝试过的菜系', weights: { food_adventure: 5, food_aesthetic: 2 } },
    ],
  },
  {
    id: 6,
    type: 'FOOD',
    content: '你的理想晚餐是？',
    active: true,
    options: [
      { id: 21, label: 'A', content: '热闹的火锅或烧烤聚会', weights: { food_social: 5, food_tradition: 3 } },
      { id: 22, label: 'B', content: '精致的 fine dining 体验', weights: { food_aesthetic: 5, food_adventure: 3 } },
      { id: 23, label: 'C', content: '妈妈做的家常菜', weights: { food_tradition: 5, food_practical: 3 } },
      { id: 24, label: 'D', content: '随心点外卖，边吃边看剧', weights: { food_practical: 5, food_adventure: 1 } },
    ],
  },
]

const TRAVEL_QUESTIONS = [
  {
    id: 7,
    type: 'TRAVEL',
    content: '理想的旅行目的地是？',
    active: true,
    options: [
      { id: 25, label: 'A', content: '繁华都市，购物美食打卡', weights: { travel_urban: 5, travel_social: 3 } },
      { id: 26, label: 'B', content: '自然风光，徒步露营看星星', weights: { travel_nature: 5, travel_adventure: 4 } },
      { id: 27, label: 'C', content: '文化古城，博物馆和历史遗迹', weights: { travel_culture: 5, travel_nature: 1 } },
      { id: 28, label: 'D', content: '海边度假村，躺平晒太阳', weights: { travel_relax: 5, travel_nature: 3 } },
    ],
  },
  {
    id: 8,
    type: 'TRAVEL',
    content: '旅行中你更喜欢？',
    active: true,
    options: [
      { id: 29, label: 'A', content: '提前做好详细攻略，按计划走', weights: { travel_structured: 5, travel_adventure: -2 } },
      { id: 30, label: 'B', content: '随性而行，走到哪算哪', weights: { travel_adventure: 5, travel_structured: -3 } },
      { id: 31, label: 'C', content: '跟团或找当地向导', weights: { travel_social: 4, travel_structured: 2 } },
      { id: 32, label: 'D', content: '找个小众地点深度体验', weights: { travel_culture: 4, travel_adventure: 3 } },
    ],
  },
]

const QUESTION_MAP: Record<string, typeof PERSONALITY_QUESTIONS> = {
  personality: PERSONALITY_QUESTIONS,
  food: FOOD_QUESTIONS,
  travel: TRAVEL_QUESTIONS,
}

const MOCK_REPORT = {
  user: MOCK_USER,
  scores: { OPENNESS: 78, CONSCIENTIOUSNESS: 65, EXTRAVERSION: 82, AGREEABLENESS: 70, NEUROTICISM: 35 },
  indicators: [
    { name: '开放性', max: 100 },
    { name: '尽责性', max: 100 },
    { name: '外向性', max: 100 },
    { name: '宜人性', max: 100 },
    { name: '情绪稳定', max: 100 },
  ],
  radarValues: [78, 65, 82, 70, 65], // NEUROTICISM inverted: 100-35=65
  interpretations: [
    '你是典型的"社交探索者"——外向且充满好奇心，在聚会中是氛围担当，也愿意尝试新鲜事物。',
    '你的宜人性得分较高，在团队中善于换位思考，但也需要注意适时坚持自己的立场。',
    '情绪稳定性较好，面对压力能保持相对从容的心态，这让你在人际关系中显得可靠。',
    '尽责性中等，说明你既能灵活应变，也会在重要事务上认真对待。',
  ],
  suggestions: [
    '🍽️ 尝试带创意融合菜的餐厅——新奇感让你更有满足感。',
    '✈️ 下次旅行可以考虑小众文化目的地，深度体验能激发你的开放性优势。',
    '💼 在职业选择上，创意类、社交类岗位比纯执行类更适合你。',
    '🧘 偶尔的高强度运动中释放压力，比纯粹休息更有效。',
    '📚 推荐阅读《安静的力量》——了解内向型特质能帮你更好地与不同类型的朋友相处。',
    '👥 在重要决策时，适当使用"利弊清单"法来补充你的直觉风格。',
  ],
  generatedAt: new Date().toISOString(),
}

const MOCK_RECOMMENDATIONS = {
  food: [
    { id: 1, scene: 'food', title: '城市天台·地中海融合菜', description: '露天用餐 + 创意摆盘，适合你爱尝鲜的美食风格。', tags: ['探索', '社交', '创意'], score: 92 },
    { id: 2, scene: 'food', title: '弄堂深处的私房小厨', description: '预约制的本帮菜私厨，地道又有仪式感。', tags: ['地道', '精致', '私密'], score: 85 },
    { id: 3, scene: 'food', title: '深夜营业的日式居酒屋', description: '适合三五好友小聚，轻松而不将就。', tags: ['社交', '日式', '轻松'], score: 78 },
  ],
  travel: [
    { id: 4, scene: 'travel', title: '云南沙溪古镇慢生活', description: '避开人群，在茶马古道上的静谧小镇深度停留。', tags: ['文化', '自然', '慢节奏'], score: 90 },
    { id: 5, scene: 'travel', title: '东京设计之旅', description: '从美术馆到买手店，满足你对美学和新鲜感的双重追求。', tags: ['都市', '设计', '新奇'], score: 83 },
    { id: 6, scene: 'travel', title: '川西自驾环线', description: '雪山草原，在广阔天地间释放探索的天性。', tags: ['冒险', '自然', '自驾'], score: 88 },
  ],
  outfit: [
    { id: 7, scene: 'outfit', title: '城市机能风', description: '多口袋马甲 + 阔腿工装裤，实用中透着个性。', tags: ['都市', '机能', '个性'], score: 87 },
    { id: 8, scene: 'outfit', title: '日系简约叠穿', description: '低饱和色系 + 层次感搭配，低调但有细节。', tags: ['简约', '日系', '质感'], score: 76 },
  ],
  career: [
    { id: 9, scene: 'career', title: '用户体验设计师', description: '你的同理心+创造力组合，在 UX 领域如鱼得水。', tags: ['创意', '技术', '共情'], score: 91 },
    { id: 10, scene: 'career', title: '品牌策划 / 内容创意', description: '开放性和社交力让你能洞察趋势、带动团队。', tags: ['创意', '社交', '趋势'], score: 86 },
    { id: 11, scene: 'career', title: '心理咨询师', description: '高宜人性和情绪稳定性是心理咨询的核心素质。', tags: ['共情', '稳定', '助人'], score: 82 },
  ],
}

const MOCK_MATCH = {
  id: 1,
  owner: MOCK_USER,
  target: { id: 2, phone: '139****0002', displayName: '安静计划家', avatarUrl: '', role: 'USER' as const, active: true },
  score: 76,
  summary: '你们性格互补，一个热情外向、一个沉稳内敛，在决策和社交上可以形成很好的互补。',
  advantages: ['社交场景中一人带动气氛、一人深度连接', '决策时直觉与分析互补', '生活节奏上一个快一个慢，能找到平衡点'],
  warnings: ['一方需要空间时，另一方的热情可能被误解为打扰', '旅行风格：spontaneous vs planned 需要提前协商', '消费观可能存在差异'],
  advice: ['尝试每月一次"对方主导日"，体验彼此的世界', '沟通中多用"我需要..."而非"你总是..."的句式', '利用差异而非消灭差异：让安静计划家负责出行攻略，让晨间探索者发掘路上的惊喜'],
  ownerScores: { OPENNESS: 78, CONSCIENTIOUSNESS: 65, EXTRAVERSION: 82, AGREEABLENESS: 70, NEUROTICISM: 35, FOOD_ADVENTURE: 50, FOOD_SOCIAL: 50, TRAVEL_ADVENTURE: 50, TRAVEL_PLANNING: 50, SOCIAL_ENERGY: 50 },
  targetScores: { OPENNESS: 55, CONSCIENTIOUSNESS: 80, EXTRAVERSION: 45, AGREEABLENESS: 72, NEUROTICISM: 60, FOOD_ADVENTURE: 40, FOOD_SOCIAL: 35, TRAVEL_ADVENTURE: 30, TRAVEL_PLANNING: 75, SOCIAL_ENERGY: 40 },
  createdAt: new Date().toISOString(),
}

const MOCK_SHARES = [
  { id: 1, token: 'abc123def456', url: 'https://app.reflectstars.dev/share/abc123def456', active: true, createdAt: new Date(Date.now() - 86400000).toISOString(), expiresAt: new Date(Date.now() + 86400000 * 6).toISOString() },
  { id: 2, token: 'xyz789ghi012', url: 'https://app.reflectstars.dev/share/xyz789ghi012', active: false, createdAt: new Date(Date.now() - 172800000).toISOString(), expiresAt: undefined, revokedAt: new Date(Date.now() - 43200000).toISOString() },
]

const MOCK_HISTORY = [
  { id: 1, report: MOCK_REPORT, summary: '社交探索者型 — 高外向性·高开放性', createdAt: new Date().toISOString() },
  { id: 2, report: { ...MOCK_REPORT, generatedAt: new Date(Date.now() - 604800000).toISOString() }, summary: '社交探索者型 — 高外向性·高开放性', createdAt: new Date(Date.now() - 604800000).toISOString() },
]

const MOCK_TESTS = [
  { id: 1, type: 'PERSONALITY', scores: MOCK_REPORT.scores, createdAt: new Date().toISOString() },
]

// ============================================================
// Mock Adapter — 替换 axios 底层适配器，直接返回本地模拟数据
// ============================================================

function mockHandler(url: string, method: string | undefined, body: Record<string, unknown>) {
  const mockHeaders = { 'content-type': 'application/json' }

  const ok = <T>(data: T) => ({
    status: 200,
    statusText: 'OK',
    headers: mockHeaders,
    config: {} as any,
    data: { code: 0, message: 'ok', data } as ApiResponse<T>,
  })

  const fallback = () => ({
    status: 200,
    statusText: 'OK',
    headers: mockHeaders,
    config: {} as any,
    data: { code: 0, message: 'mock fallback', data: null } as ApiResponse<null>,
  })

  // ---- Auth ----
  if (url === '/auth/login' && method === 'post') {
    const user = String(body.phone) === '13800000000' ? MOCK_ADMIN : MOCK_USER
    return ok({ token: 'mock-jwt-token-' + user.id, user })
  }
  if (url === '/auth/register' && method === 'post') {
    return ok({ token: 'mock-jwt-token-1', user: { ...MOCK_USER, displayName: (body.displayName as string) || '新用户' } })
  }

  // ---- User ----
  if (url === '/users/me' && method === 'get') return ok(MOCK_USER)
  if (url === '/users/me' && method === 'put') return ok({ ...MOCK_USER, ...body })

  // ---- Questions ----
  if (url?.startsWith('/questions') && method === 'get') {
    const type = new URL(url, 'http://x').searchParams.get('type') || 'personality'
    return ok(QUESTION_MAP[type] || PERSONALITY_QUESTIONS)
  }

  // ---- Tests ----
  if (url === '/tests/submit' && method === 'post') return ok(MOCK_TESTS[0])
  if (url === '/tests/history' && method === 'get') return ok(MOCK_TESTS)

  // ---- Reports ----
  if (url === '/reports/me' && method === 'get') return ok(MOCK_REPORT)
  if (url === '/reports/history' && method === 'get') return ok(MOCK_HISTORY)

  // ---- Shares ----
  if (url === '/shares/report' && method === 'post') {
    const newShare = { id: Date.now(), token: 'new' + Date.now(), url: `https://app.reflectstars.dev/share/new${Date.now()}`, active: true, createdAt: new Date().toISOString(), expiresAt: new Date(Date.now() + 86400000 * 7).toISOString() }
    return ok({ ...newShare, report: MOCK_REPORT })
  }
  if (url === '/shares' && method === 'get') return ok(MOCK_SHARES)
  if (url?.match(/\/shares\/\d+$/) && method === 'delete') return ok(null)
  if (url?.match(/\/shares\/[^/]+$/) && method === 'get') return ok(MOCK_REPORT)

  // ---- Recommendations ----
  if (url?.startsWith('/recommendations') && !url?.includes('/feedback') && method === 'get') {
    const scene = new URL(url, 'http://x').searchParams.get('scene') || 'food'
    const items = (MOCK_RECOMMENDATIONS as Record<string, typeof MOCK_RECOMMENDATIONS.food>)[scene] || MOCK_RECOMMENDATIONS.food
    return ok(items)
  }
  if (url?.match(/\/recommendations\/\d+\/feedback/) && method === 'post') return ok(null)

  // ---- Matches ----
  if (url === '/matches/invite' && method === 'post') {
    const code = 'ME' + Math.random().toString(16).slice(2, 8).toUpperCase()
    return ok({ code, createdAt: new Date().toISOString(), status: 'ACTIVE', expiresAt: new Date(Date.now() + 90 * 86400000).toISOString() })
  }
  if (url === '/matches/invites' && method === 'get') {
    const data = [{ code: 'ME1A2B3C', createdAt: new Date().toISOString(), status: 'ACTIVE' as const, expiresAt: new Date(Date.now() + 90 * 86400000).toISOString() }]
    console.log('[Mock] /matches/invites 返回:', JSON.stringify(data))
    return ok(data)
  }
  if (url === '/matches/by-invite' && method === 'post') return ok(MOCK_MATCH)
  if (url === '/matches' && method === 'post') return ok(MOCK_MATCH)
  if (url === '/matches' && method === 'get') return ok([MOCK_MATCH])
  if (url?.match(/\/matches\/\d+$/) && method === 'get') return ok(MOCK_MATCH)

  // ---- Admin ----
  if (url === '/admin/stats' && method === 'get') return ok({ users: 3, questions: 8, recommendations: 11, feedbacks: 24, matches: 1 })
  if (url === '/admin/dashboard' && method === 'get') {
    return ok({ stats: { users: 3, questions: 8, recommendations: 11, feedbacks: 24, matches: 1 }, testsByType: { PERSONALITY: 5, FOOD: 2, TRAVEL: 1 }, feedbackByRating: { LIKE: 18, NEUTRAL: 4, DISLIKE: 2 }, recommendationsByScene: { FOOD: 3, TRAVEL: 3, OUTFIT: 2, CAREER: 3 }, activeShares: 1 })
  }
  if (url === '/admin/questions' && method === 'get') return ok([...PERSONALITY_QUESTIONS, ...FOOD_QUESTIONS, ...TRAVEL_QUESTIONS])
  if (url === '/admin/feedback' && method === 'get') return ok([])
  if (url === '/admin/logs' && method === 'get') return ok([])
  if (url === '/admin/users' && method === 'get') return ok([MOCK_ADMIN, MOCK_USER, MOCK_MATCH.target])
  if (url?.match(/\/admin\/users\/\d+$/) && method === 'put') return ok(MOCK_USER)
  if (url === '/admin/recommendation-items' && method === 'get') return ok(Object.values(MOCK_RECOMMENDATIONS).flat())
  if (url?.match(/\/admin\/questions(\/\d+)?$/) && (method === 'post' || method === 'put')) return ok(PERSONALITY_QUESTIONS[0])
  if (url?.match(/\/admin\/questions\/\d+$/) && method === 'delete') return ok(null)
  if (url?.match(/\/admin\/recommendation-items(\/\d+)?$/) && (method === 'post' || method === 'put')) return ok(MOCK_RECOMMENDATIONS.food[0])
  if (url?.match(/\/admin\/recommendation-items\/\d+$/) && method === 'delete') return ok(null)
  if (url === '/admin/recommendation-rules' && method === 'get') return ok([{ id: 1, tag: 'explore', label: '探索', weight: 1.2, active: true }, { id: 2, tag: 'structured', label: '结构', weight: 0.8, active: true }, { id: 3, tag: 'social', label: '社交', weight: 1.1, active: true }, { id: 4, tag: 'gentle', label: '温和', weight: 1.0, active: true }])
  if (url?.match(/\/admin\/recommendation-rules(\/\d+)?$/) && (method === 'post' || method === 'put')) return ok({ id: 1, tag: 'explore', label: '探索', weight: 1.2, active: true })
  if (url?.match(/\/admin\/recommendation-rules\/\d+$/) && method === 'delete') return ok(null)

  // ---- Fallback ----
  console.warn(`[Mock] 未匹配的请求: ${method?.toUpperCase()} ${url}`)
  return fallback()
}

api.defaults.adapter = async (config) => {
  const { url, method, data } = config
  const body = data ? (typeof data === 'string' ? JSON.parse(data) : data) : {}
  const result = mockHandler(url || '', method || 'get', body as Record<string, unknown>)
  await delay()
  return result
}

console.log('[Mock] ✅ Mock 数据层已激活 — 所有 API 调用将返回本地模拟数据')
