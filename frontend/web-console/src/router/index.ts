import { createRouter, createWebHistory } from 'vue-router'

import AppLayout from '@/layouts/AppLayout.vue'
import OverviewView from '@/views/OverviewView.vue'
import ProjectsView from '@/views/ProjectsView.vue'
import QueryView from '@/views/QueryView.vue'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          name: 'overview',
          component: OverviewView,
        },
        {
          path: 'projects',
          name: 'projects',
          component: ProjectsView,
        },
        {
          path: 'queries',
          name: 'queries',
          component: QueryView,
        },
      ],
    },
  ],
})
