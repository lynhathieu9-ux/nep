import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as userApi from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  async function login(phone, password) {
    const res = await userApi.login(phone, password)
    const data = res.data
    user.value = data.user
    token.value = data.token
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.user.id)
    localStorage.setItem('userRole', data.user.role)
    return data
  }

  async function register(data) {
    return await userApi.register(data)
  }

  function logout() {
    user.value = null
    token.value = ''
    localStorage.clear()
  }

  async function fetchUser() {
    const userId = localStorage.getItem('userId')
    if (userId) {
      const res = await userApi.getUserById(userId)
      user.value = res.data
    }
  }

  function isLoggedIn() { return !!localStorage.getItem('token') }
  function getRole() { return localStorage.getItem('userRole') || '' }
  function isAdmin() { return getRole() === 'NEPM' }

  return { user, token, login, register, logout, fetchUser, isLoggedIn, getRole, isAdmin }
})
