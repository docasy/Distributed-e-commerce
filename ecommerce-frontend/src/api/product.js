import request from '@/utils/request'

// 分页查询商品
export const getProductPage = (params) => {
  return request({
    url: '/product/page',
    method: 'get',
    params
  })
}

// 根据ID查询商品
export const getProductById = (id) => {
  return request({
    url: `/product/${id}`,
    method: 'get'
  })
}
