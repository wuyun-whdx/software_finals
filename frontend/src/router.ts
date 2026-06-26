import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'
import HomeView from './views/HomeView.vue'
import DashboardView from './views/DashboardView.vue'
import LoginView from './views/LoginView.vue'
import TestView from './views/TestView.vue'
import ReportView from './views/ReportView.vue'
import RecommendationsView from './views/RecommendationsView.vue'
import MatchView from './views/MatchView.vue'
import MatchTabs from './views/MatchTabs.vue'
import ProfileView from './views/ProfileView.vue'
import AdminView from './views/AdminView.vue'
import ShareView from './views/ShareView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/dashboard', component: DashboardView, meta: { auth: true } },
    { path: '/login', component: LoginView },
    { path: '/tests/:type?', component: TestView, meta: { auth: true } },
    { path: '/report', component: ReportView, meta: { auth: true } },
    { path: '/recommendations', component: RecommendationsView, meta: { auth: true } },
    {
      path: '/match',
      component: MatchTabs,
      meta: { auth: true },
      children: [
        { path: '', component: MatchView },
        { path: 'friends', component: () => import('./views/FriendsView.vue') },
        { path: 'chat', component: () => import('./views/ChatView.vue') },
        { path: 'chat/:friendId', component: () => import('./views/ChatView.vue') }
      ]
    },
    { path: '/profile', component: ProfileView, meta: { auth: true } },
    { path: '/admin', component: AdminView, meta: { auth: true, admin: true } },
    { path: '/share/:token', component: ShareView },
    { path: '/community', component: () => import('./views/community/CommunityView.vue'), meta: { auth: true } },
    { path: '/community/create', component: () => import('./views/community/PostCreateView.vue'), meta: { auth: true } },
    { path: '/community/post/:id', component: () => import('./views/community/PostDetailView.vue'), meta: { auth: true } }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path === '/report' && to.query.demo === 'true') return true
  if (to.meta.auth && !auth.isAuthed) return '/login'
  if (to.meta.admin && !auth.isAdmin) return '/'
})

export default router
