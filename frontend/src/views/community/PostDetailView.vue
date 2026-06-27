<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Heart, Star, Eye, MessageCircle, Trash2 } from 'lucide-vue-next'
import EmptyState from '../../components/common/EmptyState.vue'
import LoadingState from '../../components/common/LoadingState.vue'
import PageContainer from '../../components/common/PageContainer.vue'
import { postApi } from '../../api'
import type { CommentResponse, PostResponse } from '../../types'
import { useAuthStore } from '../../stores/auth'
import { useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const post = ref<PostResponse | null>(null)
const comments = ref<CommentResponse[]>([])
const commentText = ref('')
const loading = ref(true)
const error = ref('')
const notice = ref('')
const editingPost = ref(false)
const editingCommentId = ref<number | null>(null)
const editText = ref('')

function formatTime(value: string) { return new Date(value).toLocaleString() }

function canDelete() {
  if (!post.value) return false
  return auth.isAdmin || post.value.author.id === auth.user?.id
}

async function handleDelete() {
  if (!post.value) return
  try {
    await postApi.delete(post.value.id)
    router.push('/community')
  } catch (err) {
    error.value = (err as Error).message || '删除失败'
  }
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const id = Number(route.params.id)
    post.value = await postApi.get(id)
    comments.value = await postApi.listComments(id, 0)
  } catch (err) { error.value = (err as Error).message || '加载失败' }
  finally { loading.value = false }
}

async function doLike() { await postApi.like(post.value!.id); load() }
async function doUnlike() { await postApi.unlike(post.value!.id); load() }
async function doFavorite() { await postApi.favorite(post.value!.id); load() }
async function doUnfavorite() { await postApi.unfavorite(post.value!.id); load() }
async function doComment() {
  if (!commentText.value.trim()) return
  try {
    await postApi.createComment(post.value!.id, { content: commentText.value })
    commentText.value = ''
    comments.value = await postApi.listComments(post.value!.id, 0)
  } catch (err) { error.value = (err as Error).message }
}
async function deleteComment(commentId: number) {
  await postApi.deleteComment(post.value!.id, commentId)
  comments.value = comments.value.filter(c => c.id !== commentId)
}

// Edit post
function startEditPost() { editingPost.value = true; editText.value = post.value!.content }
async function saveEditPost() {
  try { await postApi.update(post.value!.id, { content: editText.value, domainTag: '' }); post.value!.content = editText.value; editingPost.value = false; notice.value = '帖子已更新' }
  catch (err) { error.value = (err as Error).message || '更新失败' }
}

// Edit comment
function startEditComment(c: CommentResponse) { editingCommentId.value = c.id; editText.value = c.content }
async function saveEditComment(id: number) {
  try { await postApi.updateComment(id, editText.value); comments.value = comments.value.map(c => c.id === id ? { ...c, content: editText.value } : c); editingCommentId.value = null; notice.value = '评论已更新' }
  catch (err) { error.value = (err as Error).message || '更新失败' }
}

onMounted(load)
</script>

<template>
  <PageContainer :eyebrow="post?.domainTag || '帖子'" :title="post?.author.displayName + ' 的动态'">
    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="notice" class="notice">{{ notice }}</div>
    <LoadingState v-if="loading" message="正在加载帖子..." />

    <template v-if="post">
      <article class="panel">
        <template v-if="editingPost">
          <textarea v-model="editText" class="field-input" rows="3" style="width:100%"></textarea>
          <div class="toolbar" style="margin-top:8px">
            <button class="primary small" @click="saveEditPost()">保存</button>
            <button class="ghost small" @click="editingPost = false">取消</button>
          </div>
        </template>
        <template v-else>
          <p class="post-body">{{ post.content }}</p>
        </template>
        <div class="tag-row">
          <span>{{ post.domainTag }}</span>
          <span v-for="tag in post.styleTags" :key="tag">{{ tag }}</span>
        </div>
        <p v-if="post.showCompatibility" class="notice compact">你的契合度：{{ post.compatibility }}%</p>
        <div class="toolbar section-gap">
          <button class="primary small" @click="doLike"><Heart :size="14" /> {{ post.likeCount }}</button>
          <button class="secondary small" @click="doFavorite"><Star :size="14" /> {{ post.favoriteCount }}</button>
          <span class="muted"><Eye :size="14" class="icon-sm" /> {{ post.viewCount }} · <MessageCircle :size="14" class="icon-sm" /> {{ post.commentCount }}</span>
          <button v-if="canDelete()" class="secondary small" @click="startEditPost()">编辑</button>
          <button v-if="canDelete()" class="ghost small danger" @click="handleDelete()"><Trash2 :size="14" /> 删除</button>
        </div>
      </article>

      <section class="panel section-gap">
        <h2>评论 ({{ comments.length }})</h2>
        <div class="toolbar filter-bar">
          <input v-model="commentText" class="field-input" placeholder="写评论..." maxlength="500" />
          <button class="primary small" @click="doComment">发表</button>
        </div>
        <EmptyState v-if="!comments.length" title="暂无评论" description="成为第一个评论的人" />
        <div v-else v-for="c in comments" :key="c.id" class="card comment-card">
          <div class="split">
            <strong>{{ c.user.displayName }}</strong>
            <small class="muted">{{ formatTime(c.createdAt) }}</small>
          </div>
          <template v-if="editingCommentId === c.id">
            <textarea v-model="editText" class="field-input" rows="2" style="width:100%"></textarea>
            <div class="toolbar" style="margin-top:6px">
              <button class="primary small" @click="saveEditComment(c.id)">保存</button>
              <button class="ghost small" @click="editingCommentId = null">取消</button>
            </div>
          </template>
          <template v-else>
            <p>{{ c.content }}</p>
          </template>
          <div v-if="auth.isAdmin || c.user.id === auth.user?.id || post?.author.id === auth.user?.id" class="toolbar" style="margin-top:4px">
            <button v-if="c.user.id === auth.user?.id" class="secondary small" @click="startEditComment(c)">编辑</button>
            <button class="ghost small" @click="deleteComment(c.id)">删除</button>
          </div>
        </div>
      </section>
    </template>
  </PageContainer>
</template>

<style scoped>
.comment-card {
  margin-bottom: 8px;
}
</style>
