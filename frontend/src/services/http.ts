import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '../types'

const TOKEN_KEY = 'course-platform-token'
const SESSION_KEY = 'course-platform-session'
const LAST_ROUTE_KEY = 'course-platform-last-route'
let authExpiredHandled = false

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use((response) => {
  const body = response.data
  if (body && typeof body.code === 'number' && body.code !== 200) {
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败'))
  }
  return response
}, (error) => {
  if (error?.response?.status === 401) {
    handleUnauthorized()
    return Promise.reject(error)
  }
  const message = error?.response?.data?.message || error?.message || '网络请求失败'
  ElMessage.error(message)
  return Promise.reject(error)
})

function handleUnauthorized() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(SESSION_KEY)
  localStorage.removeItem(LAST_ROUTE_KEY)
  if (authExpiredHandled) return
  authExpiredHandled = true
  ElMessage.error('登录状态已失效，请重新登录')
  if (window.location.pathname !== '/login') {
    const redirect = `${window.location.pathname}${window.location.search}`
    window.location.assign(`/login?redirect=${encodeURIComponent(redirect)}`)
  }
}

export async function unwrap<T>(promise: Promise<{ data: ApiResult<T> }>): Promise<T> {
  const response = await promise
  return response.data.data
}
