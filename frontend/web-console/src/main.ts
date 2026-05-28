import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'

import './style.css'
import App from '@/App.vue'
import { router } from '@/router'

document.title = import.meta.env.VITE_APP_TITLE ?? 'AutoCode Web Console'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
