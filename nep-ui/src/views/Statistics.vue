<template>
  <div class="statistics">
    <h2 style="margin-bottom: 20px;">📊 数据统计大盘</h2>

    <!-- 实时统计 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="AQI检测累计数量" :value="10234" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="检测结果良好" :value="8234">
            <template #suffix>
              <el-icon color="#52c41a"><CircleCheck /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="检测结果超标" :value="2000">
            <template #suffix>
              <el-icon color="#f5222d"><Warning /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="全国网格覆盖率" value="78.5">
            <template #suffix>
              <span style="font-size: 16px;">%</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- AQI指数分布 -->
    <el-card shadow="never" style="margin-top: 20px;">
      <template #header>
        <span>AQI指数分布统计</span>
      </template>
      <el-table :data="distributionData" border stripe>
        <el-table-column prop="level" label="AQI等级" width="200" />
        <el-table-column prop="count" label="超标累计数量" width="150" />
        <el-table-column prop="percent" label="占比" width="150" />
        <el-table-column label="占比图">
          <template #default="{ row }">
            <el-progress :percentage="row.percentValue" :color="row.color" :stroke-width="16" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 省分组统计 -->
    <el-card shadow="never" style="margin-top: 20px;">
      <template #header>
        <span>省分组AQI超标统计</span>
      </template>
      <el-table :data="provinceStats" border stripe max-height="500">
        <el-table-column prop="province" label="省份" width="150" fixed />
        <el-table-column prop="so2" label="SO₂超标" width="120" sortable />
        <el-table-column prop="co" label="CO超标" width="120" sortable />
        <el-table-column prop="pm25" label="PM2.5超标" width="120" sortable />
        <el-table-column prop="aqi" label="AQI等级超标" width="140" sortable />
        <el-table-column label="AQI超标率">
          <template #default="{ row }">
            <el-progress :percentage="row.aqi * 100 / 500" :stroke-width="12"
              :status="row.aqi > 200 ? 'exception' : ''" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const distributionData = ref([
  { level: '一级 - 优', count: 0, percent: '0%', percentValue: 0, color: '#00e400' },
  { level: '二级 - 良', count: 0, percent: '0%', percentValue: 0, color: '#ffff00' },
  { level: '三级 - 轻度污染', count: 0, percent: '0%', percentValue: 0, color: '#ff7e00' },
  { level: '四级 - 中度污染', count: 0, percent: '0%', percentValue: 0, color: '#ff0000' },
  { level: '五级 - 重度污染', count: 0, percent: '0%', percentValue: 0, color: '#99004c' },
  { level: '六级 - 严重污染', count: 0, percent: '0%', percentValue: 0, color: '#7e0023' }
])

// 模拟省分组数据
const provinceStats = ref([
  { province: '河北省', so2: 45, co: 32, pm25: 78, aqi: 155 },
  { province: '山西省', so2: 67, co: 43, pm25: 89, aqi: 199 },
  { province: '辽宁省', so2: 34, co: 28, pm25: 56, aqi: 118 },
  { province: '江苏省', so2: 23, co: 18, pm25: 67, aqi: 108 },
  { province: '山东省', so2: 56, co: 41, pm25: 92, aqi: 189 },
  { province: '河南省', so2: 48, co: 37, pm25: 85, aqi: 170 },
  { province: '湖北省', so2: 28, co: 22, pm25: 58, aqi: 108 },
  { province: '广东省', so2: 12, co: 10, pm25: 34, aqi: 56 },
  { province: '四川省', so2: 31, co: 25, pm25: 72, aqi: 128 },
  { province: '陕西省', so2: 42, co: 35, pm25: 80, aqi: 157 }
])
</script>

<style scoped>
.statistics { max-width: 1400px; margin: 0 auto; }
.stat-row { margin-bottom: 20px; }
</style>
