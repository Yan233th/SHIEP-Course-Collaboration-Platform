import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '../types'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('course-platform-token')
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
  const message = error?.response?.data?.message || error?.message || '网络请求失败'
  ElMessage.error(message)
  return Promise.reject(error)
})

export async function unwrap<T>(promise: Promise<{ data: ApiResult<T> }>): Promise<T> {
  const response = await promise
  return response.data.data
}
