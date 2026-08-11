import api from './axios'

export function login(request) {
  return api.post('/auth/login', request)
}

export function register(request) {
  return api.post('/auth/register', request)
}
