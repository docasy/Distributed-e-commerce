import request from '@/utils/request'

export const getAddresses = () => request({ url: '/user/address', method: 'get' })

export const getAddressById = (id) => request({ url: `/user/address/${id}`, method: 'get' })

export const addAddress = (data) => request({ url: '/user/address', method: 'post', data })

export const updateAddress = (id, data) => request({ url: `/user/address/${id}`, method: 'put', data })

export const deleteAddress = (id) => request({ url: `/user/address/${id}`, method: 'delete' })

export const setDefaultAddress = (id) => request({ url: `/user/address/${id}/default`, method: 'post' })
