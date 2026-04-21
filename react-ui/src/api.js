import axios from 'axios'

export const api = axios.create({ baseURL: '' })
export const djangoApi = axios.create({ baseURL: '/django' })
