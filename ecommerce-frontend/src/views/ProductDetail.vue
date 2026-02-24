<template>
  <div class="product-detail">
    <el-card v-loading="loading">
      <el-row :gutter="20" v-if="product">
        <el-col :span="10">
          <el-image
            :src="product.mainImage || 'https://via.placeholder.com/600x400?text=' + product.name"
            fit="cover"
            style="width: 100%; border-radius: 8px;"
          />
        </el-col>
        <el-col :span="14">
          <h1>{{ product.name }}</h1>
          <p class="product-title">{{ product.title }}</p>
          <el-divider />
          <div class="info-item">
            <span class="label">价格：</span>
            <span class="price">¥{{ product.price }}</span>
          </div>
          <div class="info-item">
            <span class="label">品牌：</span>
            <span>{{ product.brand || '暂无' }}</span>
          </div>
          <div class="info-item">
            <span class="label">库存：</span>
            <span>{{ product.stock }} 件</span>
          </div>
          <div class="info-item">
            <span class="label">销量：</span>
            <span>{{ product.sales || 0 }} 件</span>
          </div>
          <el-divider />
          <div class="description">
            <h3>商品描述</h3>
            <p>{{ product.description || '暂无描述' }}</p>
          </div>
          <div class="actions">
            <el-button type="primary" size="large" @click="router.back()">
              返回
            </el-button>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProductById } from '@/api/product'

const router = useRouter()
const route = useRoute()
const product = ref(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getProductById(route.params.id)
    product.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.product-detail {
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  font-size: 28px;
  margin: 0 0 10px 0;
}

.product-title {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

.info-item {
  margin: 15px 0;
  font-size: 16px;
}

.label {
  color: #606266;
  margin-right: 10px;
}

.price {
  font-size: 32px;
  color: #f56c6c;
  font-weight: bold;
}

.description {
  margin: 20px 0;
}

.description h3 {
  margin: 0 0 10px 0;
}

.description p {
  color: #606266;
  line-height: 1.6;
}

.actions {
  margin-top: 30px;
}
</style>
