import api from './axios'

export function getMemberProfile() {
  return api.get('/member/profile')
}

export function updateMemberProfile(request) {
  return api.put('/member/profile', request)
}
