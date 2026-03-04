<template>

  <div>

    <div class="toolbar">

      <el-button type="primary" @click="openCreate" v-if="props.createUrl">新增</el-button>

      <el-input v-model="keyword" placeholder="关键字" style="width:200px;margin-left:10px" />

      <el-select v-model="community" placeholder="社区筛选" style="width:150px;margin-left:10px" v-if="showCommunityFilter" clearable>
        <el-option label="全部社区" value="" />
        <el-option
          v-for="item in communityOptions"
          :key="item"
          :label="item"
          :value="item"
        />
      </el-select>

      <el-button @click="load">查询</el-button>

    </div>

    <el-table :data="rows" v-loading="loading" style="width: 100%">

      <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width">

        <template #default="scope">

          

          <span v-if="col.computed && col.formatter">

            {{ col.formatter(scope.row) }}

          </span>

          

          <el-tag v-else-if="col.type === 'boolean'" :type="scope.row[col.prop] ? 'success' : 'info'" size="small">

            {{ scope.row[col.prop] ? '是' : '否' }}

          </el-tag>

          

          <span v-else-if="col.type === 'date'">

            {{ scope.row[col.prop] ? new Date(scope.row[col.prop]).toLocaleDateString('zh-CN') : '-' }}

          </span>

          

          <span v-else>

            {{ scope.row[col.prop] || '-' }}

          </span>

        </template>

      </el-table-column>

      <el-table-column label="操作" width="160">

        <template #default="scope">

          <el-button size="small" @click="openEdit(scope.row)" v-if="updateUrl">编辑</el-button>

          <el-button size="small" type="danger" @click="remove(scope.row)" v-if="deleteUrl">删除</el-button>

        </template>

      </el-table-column>

    </el-table>

    <el-pagination

      v-model:current-page="page"

      v-model:page-size="pageSize"

      :total="total"

      :page-sizes="[10,20,50,100]"

      layout="total, sizes, prev, pager, next, jumper"

      @current-change="load"

      @size-change="load"

      style="margin-top:10px;text-align:right"

    />



    <el-dialog v-model="show" :title="editing ? '编辑' : '新增'" width="800px">

      <el-form :model="form" label-width="120px" :rules="formRules">

        <el-row :gutter="20">

          <el-col :span="12" v-for="col in editableColumns" :key="col.prop">

            <el-form-item :label="col.label" :prop="col.required ? col.prop : undefined">

              <!-- 文本输入框 -->

              <el-input 

                v-if="!col.type || col.type === 'text'"

                v-model="form[col.prop]" 

                :placeholder="'请输入' + col.label"

              />

              <!-- 多行文本 -->

              <el-input 

                v-else-if="col.type === 'textarea'"

                v-model="form[col.prop]" 

                type="textarea"

                :rows="3"

                :placeholder="'请输入' + col.label"

              />

              <!-- 下拉选择 -->

              <el-select 

                v-else-if="col.type === 'select'"

                v-model="form[col.prop]" 

                :placeholder="'请选择' + col.label"

                style="width: 100%"

              >

                <el-option 

                  v-for="option in col.options" 

                  :key="option.value" 

                  :label="option.label" 

                  :value="option.value"

                />

              </el-select>

              <!-- 布尔值 -->

              <el-switch 

                v-else-if="col.type === 'boolean'"

                v-model="form[col.prop]"

                :active-text="'是'"

                :inactive-text="'否'"

              />

              <!-- 日期选择 -->

              <el-date-picker 

                v-else-if="col.type === 'date'"

                v-model="form[col.prop]"

                type="date"

                :placeholder="'选择' + col.label"

                style="width: 100%"

                format="YYYY-MM-DD"

                value-format="YYYY-MM-DD"

              />

              <!-- 默认文本输入 -->

              <el-input 

                v-else

                v-model="form[col.prop]" 

                :placeholder="'请输入' + col.label"

              />

            </el-form-item>

          </el-col>

        </el-row>

      </el-form>

      <template #footer>

        <el-button @click="show=false">取消</el-button>

        <el-button type="primary" @click="submit">确定</el-button>

      </template>

    </el-dialog>

  </div>

</template>



<script setup lang="ts">

import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import apiClient from '../utils/api'



const props = defineProps<{ 

  columns: any[], 

  fetchUrl: string, 

  createUrl?: string, 

  updateUrl?: string, 

  deleteUrl?: string,

  showCommunityFilter?: boolean 

}>()



const rows = ref<any[]>([])

const loading = ref(false)

const page = ref(1)

const pageSize = ref(10)

const total = ref(0)

const keyword = ref('')

const community = ref('')
// 社区选项列表，初始为空数组，等待API加载或设置默认值
const communityOptions = ref<string[]>([])



const show = ref(false)

const editing = ref(false)

const form = ref<any>({})

const editableColumns = computed(() => props.columns.filter(c => c.editable !== false && c.prop !== 'id'))





const formRules = computed(() => {

  const rules: any = {}

  props.columns.forEach(col => {

    if (col.required) {

      rules[col.prop] = [{ required: true, message: `请输入${col.label}`, trigger: 'blur' }]

    }

  })

  return rules

})





const loadCommunityOptions = async () => {
  try {
    // 清除之前的缓存，确保获取最新数据
    localStorage.removeItem('community_options')
    
    console.log('从服务器加载社区选项...')
    // 移除'/api'前缀，因为apiClient已经配置了baseURL为'/api'
    const response = await apiClient.get('users/communities')
    // 直接使用response，因为axios响应拦截器已经返回了response.data
    communityOptions.value = Array.isArray(response) ? response : []
    console.log('服务器返回社区选项:', communityOptions.value)
    
    // 不设置默认社区选项，只使用从API获取的真实数据
    
    // 保存到缓存
    localStorage.setItem('community_options', JSON.stringify({
      data: communityOptions.value,
      timestamp: Date.now()
    }))
  } catch (error) {
    console.error('加载社区列表失败:', error)
    // 加载失败时保持communityOptions为空数组，不使用默认选项
    communityOptions.value = []
    console.log('加载失败，社区选项保持为空')
  } finally {
    console.log('社区选项最终列表:', communityOptions.value)
  }
}

const load = async () => {
    loading.value = true
    try {
      let data: any = []
      const url = props.fetchUrl
      const params: any = { 
        page: page.value, 
        pageSize: pageSize.value, 
        keyword: keyword.value 
      }
      
      if (props.showCommunityFilter && community.value) {
        params.community = community.value
        console.log('添加社区筛选参数:', community.value)
      }
      
      console.log('发送API请求:', url, params)
      const resp = await apiClient.get(url, { params }) as any
      console.log('API响应:', resp)
      
      // 改进的后端分页数据格式处理逻辑
      if (resp && typeof resp === 'object') {
        // 检查是否是标准的分页响应格式
        if ('records' in resp && 'total' in resp) {
          // 后端已经处理过分页的情况
          rows.value = resp.records || []
          total.value = resp.total || 0
          console.log('使用标准分页格式，记录数:', rows.value.length, '总数:', total.value)
        } else {
          // 兼容其他可能的响应格式
          data = resp.data || resp || []
          total.value = resp.total || data.length
          rows.value = Array.isArray(data) ? data : []
          console.log('使用兼容格式，记录数:', rows.value.length, '总数:', total.value)
        }
      } else if (Array.isArray(resp)) {
        // 兼容旧的直接返回数组的情况
        data = resp
        total.value = data.length
        rows.value = data.slice((page.value-1)*pageSize.value, page.value*pageSize.value)
        console.log('使用数组格式，记录数:', rows.value.length, '总数:', total.value)
      } else {
        // 默认处理
        rows.value = []
        total.value = 0
        console.log('无法识别的响应格式，设置为空数据')
      }
    } catch (error) {
      console.error('加载数据失败:', error)
      ElMessage.error('加载数据失败，请稍后重试')
      rows.value = []
      total.value = 0
    } finally {
      loading.value = false
    }

  }



const openCreate = () => { 

  editing.value = false

  form.value = {}

  

  props.columns.forEach(col => {

    if (col.type === 'boolean' && form.value[col.prop] === undefined) {

      form.value[col.prop] = false

    }

  })

  show.value = true 

}



const openEdit = (row: any) => { 

  editing.value = true

  form.value = { ...row }

  

  props.columns.forEach(col => {

    if (col.type === 'boolean' && form.value[col.prop] === undefined) {

      form.value[col.prop] = false

    }

  })

  show.value = true 

}



const submit = async () => {
    try {
      const base = editing.value ? props.updateUrl : props.createUrl
      if (!base) return
      
      const method = editing.value ? 'put' : 'post'
      const url = editing.value ? `${base}/${form.value.id}` : base
      
      await apiClient[method](url, form.value)
      
      ElMessage.success(editing.value ? '更新成功' : '创建成功')
      show.value = false
      await load()
    } catch (error) {
      console.error('保存失败:', error)
      ElMessage.error('保存失败，请稍后重试')
    }
  }



const remove = async (row: any) => {

  try {

    await ElMessageBox.confirm('确定要删除这条记录吗？', '确认删除', {

      confirmButtonText: '确定',

      cancelButtonText: '取消',

      type: 'warning'

    })

    

    if (!props.deleteUrl) return

    await apiClient.delete(`${props.deleteUrl}/${row.id}`)

    

    ElMessage.success('删除成功')

    await load()

  } catch (error) {

    if (error !== 'cancel') {

      console.error('删除失败:', error)

      ElMessage.error('删除失败，请稍后重试')

    }

  }

}



// 组件挂载时加载社区列表和数据
onMounted(async () => {
  if (props.showCommunityFilter) {
    await loadCommunityOptions()
  }
  await load()
})

// 添加社区变化监听器，当社区选择变化时自动重新加载数据
watch(community, (newVal) => {
  console.log('社区选择变化:', newVal)
  load()
});

</script>



<style scoped>

.toolbar { margin-bottom: 10px; }

</style>





