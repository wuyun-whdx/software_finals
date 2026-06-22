import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles.css'

async function init() {
  if (import.meta.env.VITE_MOCK === 'true') {
    await import('./mock')
  }

  createApp(App).use(createPinia()).use(router).mount('#app')
}

init()

