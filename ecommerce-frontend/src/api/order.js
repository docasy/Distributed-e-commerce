import request from '@/utils/request'

// 生成幂等性Token
export const generateIdempotentToken = () => {
  return request({
    url: '/order/idempotent-token',
    method: 'get'
  })
}

// 创建订单
export const createOrder = (data) => {
  return request({
    url: '/order/create',
    method: 'post',
    data
  })
}

// 支付订单
export const payOrder = (orderNo) => {
  return request({
    url: `/order/pay/${orderNo}`,
    method: 'post'
  })
}

// 取消订单
export const cancelOrder = (orderNo) => {
  return request({
    url: `/order/cancel/${orderNo}`,
    method: 'post'
  })
}

// 查询订单详情
export const getOrderDetail = (orderNo) => {
  return request({
    url: `/order/${orderNo}`,
    method: 'get'
  })
}

// 查询我的订单
export const getMyOrders = (params) => {
  return request({
    url: '/order/my-orders',
    method: 'get',
    params
  })
}
