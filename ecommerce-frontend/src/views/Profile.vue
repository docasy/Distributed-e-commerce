<template>
  <div class="profile">
    <el-card>
      <template #header><h2 style="margin:0">个人信息</h2></template>
      <el-descriptions :column="1" border v-if="userInfo">
        <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">
          <template v-if="nicknameEdit">
            <el-input v-model="newNickname" size="small" style="width:200px" />
            <el-button type="primary" size="small" @click="handleSaveNickname" style="margin-left:8px">保存</el-button>
            <el-button size="small" @click="nicknameEdit = false">取消</el-button>
          </template>
          <template v-else>
            {{ userInfo.nickname || '-' }}
            <el-button size="small" @click="nicknameEdit = true; newNickname = userInfo.nickname" style="margin-left:8px">修改</el-button>
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userInfo.phone || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top:20px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <h2 style="margin:0">收货地址</h2>
          <el-button type="primary" @click="openDialog(null)">添加地址</el-button>
        </div>
      </template>
      <el-table :data="addresses" v-loading="addrLoading" empty-text="暂无地址，请添加">
        <el-table-column prop="receiverName" label="收货人" width="100" />
        <el-table-column prop="receiverPhone" label="电话" width="130" />
        <el-table-column label="地址">
          <template #default="{ row }">{{ row.province }}{{ row.city }}{{ row.district }} {{ row.detail }}</template>
        </el-table-column>
        <el-table-column prop="isDefault" label="默认" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault===1" type="success" size="small">默认</el-tag>
            <el-button v-else size="small" @click="handleSetDefault(row.id)">设为默认</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="addrDialog" :title="editingAddr ? '编辑地址' : '添加地址'" width="500px">
      <el-form :model="addrForm" label-width="80px">
        <el-form-item label="收货人">
          <el-input v-model="addrForm.receiverName" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="addrForm.receiverPhone" />
        </el-form-item>
        <el-form-item label="省">
          <el-input v-model="addrForm.province" />
        </el-form-item>
        <el-form-item label="市">
          <el-input v-model="addrForm.city" />
        </el-form-item>
        <el-form-item label="区">
          <el-input v-model="addrForm.district" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="addrForm.detail" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addrDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address'
import { updateProfile, getUserInfo } from '@/api/user'

const userInfo = ref(null)
const nicknameEdit = ref(false)
const newNickname = ref('')
const addresses = ref([])
const addrLoading = ref(false)
const addrDialog = ref(false)
const editingAddr = ref(null)

const addrForm = ref({
  receiverName: '', receiverPhone: '', province: '', city: '', district: '', detail: ''
})

onMounted(async () => {
  try {
    const res = await getUserInfo()
    userInfo.value = res.data
    localStorage.setItem('userInfo', JSON.stringify(res.data))
  } catch (e) { console.error(e) }
  loadAddresses()
})

const handleSaveNickname = async () => {
  try {
    await updateProfile({ nickname: newNickname.value })
    userInfo.value.nickname = newNickname.value
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    ElMessage.success('昵称已更新')
    nicknameEdit.value = false
  } catch (e) { console.error(e) }
}

const loadAddresses = async () => {
  addrLoading.value = true
  try {
    const res = await getAddresses()
    addresses.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    addrLoading.value = false
  }
}

const openDialog = (row) => {
  editingAddr.value = row
  if (row) {
    addrForm.value = { ...row }
  } else {
    addrForm.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detail: '' }
  }
  addrDialog.value = true
}

const handleSaveAddress = async () => {
  const data = { ...addrForm.value }
  try {
    if (editingAddr.value) {
      await updateAddress(editingAddr.value.id, data)
    } else {
      await addAddress(data)
    }
    ElMessage.success('保存成功')
    addrDialog.value = false
    loadAddresses()
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该地址？', '提示', { type: 'warning' })
    await deleteAddress(id)
    ElMessage.success('已删除')
    loadAddresses()
  } catch (error) {
    if (error !== 'cancel') console.error(error)
  }
}

const handleSetDefault = async (id) => {
  await setDefaultAddress(id)
  ElMessage.success('已设为默认地址')
  loadAddresses()
}
</script>

<style scoped>
.profile { max-width: 800px; margin: 0 auto; }
</style>
