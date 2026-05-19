<template>
  <div>
    <div style="margin: 10px 0">
      <el-row :gutter="20">
        <el-col :span="12">
          <div id="hotChart" style="width: 100%; height: 500px"></div>
        </el-col>
        <el-col :span="12">
          <div id="rateChart" style="width: 100%; height: 500px"></div>
        </el-col>
      </el-row>
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="24">
          <el-card>
            <div slot="header">
              <span style="font-size: 18px; font-weight: bold">选课饱和度</span>
              <el-button type="primary" size="small" icon="el-icon-refresh" @click="loadSaturation" style="float: right">刷新</el-button>
            </div>
            <el-row :gutter="20">
              <el-col :span="6" v-for="(item, index) in saturationList" :key="index" style="margin-bottom: 20px">
                <el-card shadow="hover">
                  <div style="margin-bottom: 10px; font-weight: bold; color: #409EFF">
                    {{ item.courseName }}
                  </div>
                  <div :id="'saturation-' + index" style="width: 100%; height: 300px"></div>
                  <div style="margin-top: 10px; font-size: 14px; color: #666">
                    已选：<span style="color: #F56C6C; font-weight: bold">{{ item.enrolled }}</span> / 
                    容量：<span style="color: #67C23A; font-weight: bold">{{ item.capacity || '∞' }}</span>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: "Statistics",
  data() {
    return {
      hotChart: null,
      rateChart: null,
      saturationChart: null,
      saturationList: []
    }
  },
  mounted() {
    this.initHotChart()
    this.initRateChart()
    this.loadSaturation()
  },
  beforeDestroy() {
    if (this.hotChart) this.hotChart.dispose()
    if (this.rateChart) this.rateChart.dispose()
    if (this.saturationChart) this.saturationChart.dispose()
  },
  methods: {
    initHotChart() {
      this.request.get("/statistics/hotCourses").then(res => {
        if (res.code === '200') {
          const courses = res.data
          const courseNames = courses.map(c => c.name)
          const enrolledCounts = courses.map(c => c.enrolled || 0)

          const option = {
            title: {
              text: '热门课程排行',
              subtext: 'TOP 10',
              left: 'center'
            },
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              }
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: courseNames,
              axisLabel: {
                rotate: 45,
                interval: 0
              }
            },
            yAxis: {
              type: 'value',
              name: '选课人数'
            },
            series: [
              {
                name: '选课人数',
                type: 'bar',
                data: enrolledCounts,
                itemStyle: {
                  color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: '#83bff6' },
                    { offset: 0.5, color: '#188df0' },
                    { offset: 1, color: '#188df0' }
                  ])
                },
                emphasis: {
                  itemStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                      { offset: 0, color: '#2378f9' },
                      { offset: 0.7, color: '#2378f9' },
                      { offset: 1, color: '#83bff6' }
                    ])
                  }
                },
                label: {
                  show: true,
                  position: 'top'
                }
              }
            ]
          }

          const chartDom = document.getElementById('hotChart')
          this.hotChart = echarts.init(chartDom)
          this.hotChart.setOption(option)
        }
      })
    },
    initRateChart() {
      this.request.get("/statistics/rateDistribution").then(res => {
        if (res.code === '200') {
          const data = res.data
          const total = data.total || 0
          const rateData = [
            { value: data['5星'] || 0, name: '5星', itemStyle: { color: '#67C23A' } },
            { value: data['4星'] || 0, name: '4星', itemStyle: { color: '#409EFF' } },
            { value: data['3星'] || 0, name: '3星', itemStyle: { color: '#E6A23C' } },
            { value: data['2星'] || 0, name: '2星', itemStyle: { color: '#F56C6C' } },
            { value: data['1星'] || 0, name: '1星', itemStyle: { color: '#F56C6C' } }
          ]

          const option = {
            title: {
              text: '评价星级分布',
              subtext: '总评价数: ' + total,
              left: 'center'
            },
            tooltip: {
              trigger: 'item',
              formatter: '{a} <br/>{b}: {c} ({d}%)'
            },
            legend: {
              orient: 'vertical',
              left: 'left'
            },
            series: [
              {
                name: '星级分布',
                type: 'pie',
                radius: ['40%', '70%'],
                avoidLabelOverlap: false,
                itemStyle: {
                  borderRadius: 10,
                  borderColor: '#fff',
                  borderWidth: 2
                },
                label: {
                  show: true,
                  formatter: '{b}: {c} ({d}%)'
                },
                emphasis: {
                  label: {
                    show: true,
                    fontSize: 20,
                    fontWeight: 'bold'
                  }
                },
                data: rateData
              }
            ]
          }

          const chartDom = document.getElementById('rateChart')
          this.rateChart = echarts.init(chartDom)
          this.rateChart.setOption(option)
        }
      })
    },
    loadSaturation() {
      this.request.get("/statistics/saturation").then(res => {
        if (res.code === '200') {
          this.saturationList = res.data
          this.$nextTick(() => {
            this.initSaturationCharts()
          })
        }
      })
    },
    initSaturationCharts() {
      this.saturationList.forEach((item, index) => {
        const chartDom = document.getElementById('saturation-' + index)
        if (chartDom) {
          const saturation = item.saturation || 0
          let color = '#67C23A'
          if (saturation >= 60 && saturation < 80) {
            color = '#E6A23C'
          } else if (saturation >= 80) {
            color = '#F56C6C'
          }

          const option = {
            series: [
              {
                type: 'gauge',
                startAngle: 180,
                endAngle: 0,
                min: 0,
                max: 100,
                splitNumber: 5,
                axisLine: {
                  lineStyle: {
                    width: 10,
                    color: [
                      [0.3, '#67C23A'],
                      [0.7, '#E6A23C'],
                      [1, '#F56C6C']
                    ]
                  }
                },
                pointer: {
                  icon: 'path://M12.8,0.7l12,40.1H8.0l-4-12.3l-3.5,3.5l0.1-0.1l4.8-4.8l12.4,3.5l3.5-3.5l0.1,0.1l-4.8,4.8z',
                  length: '12%',
                  width: 20,
                  offsetCenter: [0, '-60%'],
                  itemStyle: {
                    color: 'auto'
                  }
                },
                axisTick: {
                  length: 12,
                  lineStyle: {
                    color: 'auto',
                    width: 2
                  }
                },
                splitLine: {
                  length: 20,
                  lineStyle: {
                    color: 'auto',
                    width: 5
                  }
                },
                axisLabel: {
                  color: '#464646',
                  fontSize: 20,
                  distance: -60,
                  formatter: '{value}%'
                },
                title: {
                  offsetCenter: [0, '-20%'],
                  fontSize: 20
                },
                detail: {
                  fontSize: 30,
                  offsetCenter: [0, '0%'],
                  valueAnimation: true,
                  formatter: '{value}%',
                  color: 'auto'
                },
                data: [
                  {
                    value: saturation
                  }
                ]
              }
            ]
          }

          const chart = echarts.init(chartDom)
          chart.setOption(option)
        }
      })
    }
  }
}
</script>

<style scoped>
.el-card {
  margin-bottom: 20px;
}
</style>
