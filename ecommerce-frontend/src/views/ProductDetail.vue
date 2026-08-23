<template>
  <div class="product-detail">
    <el-card v-loading="loading">
      <el-row :gutter="20" v-if="product">
        <el-col :span="10">
          <el-image
            :src="product.mainImage || 'https://via.placeholder.com/600x400?text=' + product.name"
            fit="cover"
            style="width: 100%; border-radius: 8px"
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
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="product.stock"
              size="large"
              style="margin-right: 16px"
            />
            <div style="display:inline-block;min-width:220px;margin-right:16px">
              <el-select v-model="selectedAddressId" placeholder="选择收货地址" size="large" v-if="addresses.length > 0">
                <el-option
                  v-for="a in addresses"
                  :key="a.id"
                  :label="a.receiverName + ' — ' + (a.province||'') + (a.city||'') + (a.district||'') + ' ' + a.detail"
                  :value="a.id"
                />
              </el-select>
              <el-button v-else size="large" @click="router.push('/profile')">请先添加收货地址</el-button>
            </div>
            <el-button type="danger" size="large" :loading="buying" :disabled="!selectedAddressId" @click="handleBuy">立即购买</el-button>
            <el-button size="large" @click="router.back()">返回</el-button>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductById } from '@/api/product'
import { generateIdempotentToken, createOrder } from '@/api/order'
import { getAddresses } from '@/api/address'

const router = useRouter()
const route = useRoute()
const product = ref(null)
const loading = ref(false)
const buying = ref(false)
const quantity = ref(1)
const addresses = ref([])
const selectedAddressId = ref(null)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getProductById(route.params.id)
    product.value = res.data
    const addrRes = await getAddresses()
    addresses.value = addrRes.data || []
    if (addresses.value.length > 0) {
      const def = addresses.value.find(a => a.isDefault === 1)
      selectedAddressId.value = def ? def.id : addresses.value[0].id
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
})

const handleBuy = async () => {
  if (!selectedAddressId.value) {
    ElMessage.warning('请先添加收货地址')
    return
  }
  buying.value = true
  try {
    const tokenRes = await generateIdempotentToken()
    await createOrder({
      productId: product.value.id,
      quantity: quantity.value,
      addressId: selectedAddressId.value,
      idempotentToken: tokenRes.data
    })
    ElMessage.success('下单成功！')
    router.push('/orders')
  } catch (error) {
    console.error(error)
  } finally {
    buying.value = false
  }
}
</script>

<style scoped>
.product-detail {
  max-width: 1200px;
  margin: 0 auto;
}
h1 { font-size: 28px; margin: 0 0 10px 0; }
.product-title { font-size: 16px; color: #909399; margin: 0; }
.info-item { margin: 15px 0; font-size: 16px; }
.label { color: #606266; margin-right: 10px; }
.price { font-size: 32px; color: #f56c6c; font-weight: bold; }
.description { margin: 20px 0; }
.description h3 { margin: 0 0 10px 0; }
.description p { color: #606266; line-height: 1.6; }
.actions { margin-top: 30px; }
</style>
