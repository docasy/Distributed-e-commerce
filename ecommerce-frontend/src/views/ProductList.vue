<template>
  <div class="product-list">
    <el-card>
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入商品名称"
            clearable
            @clear="handleSearch"
          >
            <template #append>
              <el-button icon="Search" @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
    </el-card>
    
    <div class="product-grid">
      <el-card
        v-for="product in productList"
        :key="product.id"
        class="product-card"
        shadow="hover"
        @click="goToDetail(product.id)"
      >
        <div class="product-image">
          <el-image
            :src="product.mainImage || 'https://via.placeholder.com/300x200?text=' + product.name"
            fit="cover"
          />
        </div>
        <div class="product-info">
          <h3 class="product-title">{{ product.name }}</h3>
          <p class="product-desc">{{ product.title }}</p>
          <div class="product-footer">
            <span class="product-price">¥{{ product.price }}</span>
            <span class="product-stock">库存: {{ product.stock }}</span>
          </div>
          <el-button type="primary" @click.stop="handleBuy(product)" style="width: 100%; margin-top: 10px;">
            立即购买
          </el-button>
        </div>
      </el-card>
    </div>
    
    <el-pagination
      v-if="total > 0"
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next, jumper"
      @current-change="getProductList"
      style="margin-top: 20px; text-align: center;"
    />
    
    <!-- 购买对话框 -->
    <el-dialog v-model="buyDialogVisible" title="购买商品" width="500px">
      <el-form v-if="currentProduct" label-width="100px">
        <el-form-item label="商品名称">
          <el-input :value="currentProduct.name" disabled />
        </el-form-item>
        <el-form-item label="单价">
          <el-input :value="'¥' + currentProduct.price" disabled />
        </el-form-item>
        <el-form-item label="购买数量">
          <el-input-number v-model="orderForm.quantity" :min="1" :max="currentProduct.stock" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-select v-model="orderForm.addressId" placeholder="选择地址" style="width:100%" v-if="addresses.length > 0">
            <el-option
              v-for="a in addresses"
              :key="a.id"
              :label="a.receiverName + ' — ' + (a.province||'') + (a.city||'') + (a.district||'') + ' ' + a.detail"
              :value="a.id"
            />
          </el-select>
          <el-button v-else type="primary" size="small" @click="router.push('/profile');buyDialogVisible=false">
            请先添加收货地址
          </el-button>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
        <el-form-item label="总价">
          <span style="color: #f56c6c; font-size: 20px; font-weight: bold;">
            ¥{{ (currentProduct.price * orderForm.quantity).toFixed(2) }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="buyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitOrder" :loading="submitLoading" :disabled="!orderForm.addressId">
          确认购买
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductPage } from '@/api/product'
import { generateIdempotentToken, createOrder } from '@/api/order'
import { getAddresses } from '@/api/address'

const router = useRouter()
const productList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const searchKeyword = ref('')
const buyDialogVisible = ref(false)
const currentProduct = ref(null)
const submitLoading = ref(false)
const addresses = ref([])

const orderForm = reactive({
  quantity: 1,
  addressId: null,
  remark: ''
})

onMounted(() => {
  getProductList()
})

const getProductList = async () => {
  try {
    const res = await getProductPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value
    })
    productList.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error(error)
  }
}

const handleSearch = () => {
  pageNum.value = 1
  getProductList()
}

const goToDetail = (id) => {
  router.push(`/products/${id}`)
}

const handleBuy = async (product) => {
  currentProduct.value = product
  orderForm.quantity = 1
  const res = await getAddresses()
  addresses.value = res.data || []
  if (addresses.value.length > 0) {
    const def = addresses.value.find(a => a.isDefault === 1)
    orderForm.addressId = def ? def.id : addresses.value[0].id
  } else {
    orderForm.addressId = null
  }
  buyDialogVisible.value = true
}

const handleSubmitOrder = async () => {
  if (!orderForm.addressId) {
    ElMessage.warning('请选择收货地址')
    return
  }

  submitLoading.value = true

  try {
    const tokenRes = await generateIdempotentToken()
    await createOrder({
      productId: currentProduct.value.id,
      quantity: orderForm.quantity,
      addressId: orderForm.addressId,
      remark: orderForm.remark,
      idempotentToken: tokenRes.data
    })

    ElMessage.success('订单创建成功')
    buyDialogVisible.value = false
    router.push('/orders')
  } catch (error) {
    console.error(error)
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.product-list {
  max-width: 1400px;
  margin: 0 auto;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.product-card:hover {
  transform: translateY(-5px);
}

.product-image {
  height: 200px;
  overflow: hidden;
  border-radius: 4px;
}

.product-image :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.product-info {
  margin-top: 15px;
}

.product-title {
  font-size: 16px;
  font-weight: bold;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 12px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
}

.product-stock {
  font-size: 14px;
  color: #909399;
}
</style>
