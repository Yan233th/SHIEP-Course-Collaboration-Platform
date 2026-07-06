import axios from 'axios'
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
    return Promise.reject(new Error(body.message || '请求失败'))
  }
  return response
})

export async function unwrap<T>(promise: Promise<{ data: ApiResult<T> }>): Promise<T> {
  const response = await promise
  return response.data.data
}
