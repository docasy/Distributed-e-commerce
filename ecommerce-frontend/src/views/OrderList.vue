<template>
  <div class="order-list">
    <el-card>
      <template #header>
        <h2 style="margin: 0;">我的订单</h2>
      </template>
      
      <el-table :data="orderList" v-loading="loading" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="productName" label="商品名称" width="200" />
        <el-table-column prop="productPrice" label="单价" width="100">
          <template #default="{ row }">
            ¥{{ row.productPrice }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="totalAmount" label="总价" width="120">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold;">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="primary"
              size="small"
              @click="handlePay(row)"
            >
              支付
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              size="small"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
            <el-button size="small" @click="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="getOrderList"
        style="margin-top: 20px; text-align: center;"
      />
    </el-card>
    
    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号" :span="2">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="商品名称" :span="2">{{ currentOrder.productName }}</el-descriptions-item>
        <el-descriptions-item label="单价">¥{{ currentOrder.productPrice }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentOrder.quantity }}</el-descriptions-item>
        <el-descriptions-item label="总价" :span="2">
          <span style="color: #f56c6c; font-size: 18px; font-weight: bold;">
            ¥{{ currentOrder.totalAmount }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusType(currentOrder.status)">
            {{ getStatusText(currentOrder.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">
          {{ currentOrder.paymentMethod ? '支付宝' : '未支付' }}
        </el-descriptions-item>
        <el-descriptions-item label="收货人">{{ currentOrder.receiver }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentOrder.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ currentOrder.address }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '无' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间" :span="2">
          {{ currentOrder.paymentTime || '未支付' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyOrders, payOrder, cancelOrder } from '@/api/order'

const orderList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const detailDialogVisible = ref(false)
const currentOrder = ref(null)

const statusMap = {
  0: '待支付',
  1: '已支付',
  2: '已取消',
  3: '已完成',
  4: '已关闭'
}

const statusTypeMap = {
  0: 'warning',
  1: 'success',
  2: 'info',
  3: 'success',
  4: 'danger'
}

onMounted(() => {
  getOrderList()
})

const getOrderList = async () => {
  loading.value = true
  try {
    const res = await getMyOrders({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    orderList.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  return statusMap[status] || '未知'
}

const getStatusType = (status) => {
  return statusTypeMap[status] || 'info'
}

const handlePay = async (order) => {
  try {
    await ElMessageBox.confirm(
      `确认支付订单 ${order.orderNo}？金额：¥${order.totalAmount}`,
      '支付确认',
      {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await payOrder(order.orderNo)
    ElMessage.success('支付成功')
    getOrderList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm(
      `确认取消订单 ${order.orderNo}？`,
      '取消订单',
      {
        confirmButtonText: '确认',
        cancelButtonText: '返回',
        type: 'warning'
      }
    )
    
    await cancelOrder(order.orderNo)
    ElMessage.success('订单已取消')
    getOrderList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleViewDetail = (order) => {
  currentOrder.value = order
  detailDialogVisible.value = true
}
</script>

<style scoped>
.order-list {
  max-width: 1400px;
  margin: 0 auto;
}
</style>
