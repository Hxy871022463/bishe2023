<template>
    <div>
        <el-row :gutter="10" style="margin-bottom: 60px">
            <el-col :span="6">
                <el-card style="color: #409EFF">
                    <div><i class="el-icon-user-solid" />用户总数</div>
                    <div style="padding: 10px 0; text-align: center; font-weight: bold">
                        100
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card style="color: #F56C6C">
                    <div><i class="el-icon-money" /> 销售总量</div>
                    <div style="padding: 10px 0; text-align: center; font-weight: bold">
                        ￥1000000
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card style="color: #67C23A">
                    <div><i class="el-icon-bank-card" /> 收益总额</div>
                    <div style="padding: 10px 0; text-align: center; font-weight: bold">
                        ￥300000
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card style="color: #E6A23C">
                    <div><i class="el-icon-s-shop" /> 门店总数</div>
                    <div style="padding: 10px 0; text-align: center; font-weight: bold">
                        50
                    </div>
                </el-card>
            </el-col>
        </el-row>
        
        <el-row :gutter="20">
            <el-col :span="12">
                <el-card>
                    <div style="font-size: 16px; font-weight: bold; margin-bottom: 10px">热门课程排行（TOP 10）</div>
                    <div id="hotChart" style="width: 100%; height: 280px"></div>
                </el-card>
            </el-col>
            <el-col :span="12">
                <el-card>
                    <div slot="header">
                        <span style="font-size: 14px; font-weight: bold">评价星级分布</span>
                        <el-select v-model="selectedCourseId" placeholder="选择课程" size="mini" clearable @change="initRateChart" style="width: 200px; float: right">
                            <el-option label="全部课程" :value="null"></el-option>
                            <el-option v-for="course in courseList" :key="course.id" :label="course.name" :value="course.id"></el-option>
                        </el-select>
                    </div>
                    <div id="rateChart" style="width: 100%; height: 280px"></div>
                </el-card>
            </el-col>
        </el-row>
        
        <el-row :gutter="20" style="margin-top: 10px">
            <el-col :span="24">
                <el-card>
                    <div slot="header">
                        <span style="font-size: 14px; font-weight: bold">选课饱和度</span>
                        <el-button type="primary" size="mini" icon="el-icon-refresh" @click="loadSaturation" style="float: right">刷新</el-button>
                    </div>
                    <el-row :gutter="15">
                        <el-col :span="8" v-for="(item, index) in saturationList" :key="index" style="margin-bottom: 10px">
                            <el-card shadow="hover" style="height: 240px">
                                <div style="margin-bottom: 2px; font-weight: bold; color: #409EFF; font-size: 12px">
                                    {{ item.courseName }}
                                </div>
                                <div :id="'saturation-' + index" style="width: 100%; height: 200px"></div>
                                <div style="margin-top: -30px; font-size: 11px; color: #666">
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
</template>

<script>
import * as echarts from 'echarts'

export default {
    name: "Home.vue",
    data() {
        return {
            hotChart: null,
            rateChart: null,
            saturationList: [],
            selectedCourseId: null,
            courseList: []
        }
    },
    mounted() {
        this.loadCourseList()
        this.initHotChart()
        this.initRateChart()
        this.loadSaturation()
    },
    beforeDestroy() {
        if (this.hotChart) this.hotChart.dispose()
        if (this.rateChart) this.rateChart.dispose()
    },
    methods: {
        loadCourseList() {
            this.request.get("/course").then(res => {
                if (res.code === '200') {
                    this.courseList = res.data.records || res.data || []
                }
            })
        },
        initHotChart() {
            console.log("开始加载热门课程数据...")
            this.request.get("/statistics/hotCourses").then(res => {
                console.log("热门课程API返回:", res)
                if (res.code === '200') {
                    const courses = res.data
                    console.log("解析后的课程数据:", courses)
                    console.log("课程数组长度:", courses ? courses.length : 0)
                    if (!courses || courses.length === 0) {
                        console.warn("热门课程数据为空！")
                        return
                    }
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
            const params = this.selectedCourseId ? { courseId: this.selectedCourseId } : {}
            this.request.get("/statistics/rateDistribution", { params }).then(res => {
                if (res.code === '200') {
                    const data = res.data
                    const total = data.total || 0
                    const rateData = [
                        { value: data['5星'] || 0, name: '5星', itemStyle: { color: '#67C23A' } },
                        { value: data['4星'] || 0, name: '4星', itemStyle: { color: '#409EFF' } },
                        { value: data['3星'] || 0, name: '3星', itemStyle: { color: '#FFD54F' } },
                        { value: data['2星'] || 0, name: '2星', itemStyle: { color: '#E6A23C' } },
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
            console.log("开始加载饱和度数据...")
            this.request.get("/statistics/saturation").then(res => {
                console.log("饱和度API返回:", res)
                if (res.code === '200') {
                    this.saturationList = res.data
                    console.log("解析后的饱和度数据:", this.saturationList)
                    console.log("饱和度数组长度:", this.saturationList ? this.saturationList.length : 0)
                    if (!this.saturationList || this.saturationList.length === 0) {
                        console.warn("饱和度数据为空！")
                        return
                    }
                    this.$nextTick(() => {
                        this.initSaturationCharts()
                    })
                }
            })
        },
        initSaturationCharts() {
            console.log("开始初始化饱和度图表，数量:", this.saturationList.length)
            this.saturationList.forEach((item, index) => {
                const chartDom = document.getElementById('saturation-' + index)
                console.log(`尝试初始化图表 ${index}, DOM元素:`, chartDom)
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
                                        width: 8,
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
                                    width: 12,
                                    offsetCenter: [0, '-60%'],
                                    itemStyle: {
                                        color: 'auto'
                                    }
                                },
                                axisTick: {
                                    length: 8,
                                    lineStyle: {
                                        color: 'auto',
                                        width: 2
                                    }
                                },
                                splitLine: {
                                    length: 12,
                                    lineStyle: {
                                        color: 'auto',
                                        width: 3
                                    }
                                },
                                axisLabel: {
                                    color: '#464646',
                                    fontSize: 12,
                                    distance: -45,
                                    formatter: '{value}%'
                                },
                                title: {
                                    offsetCenter: [0, '-20%'],
                                    fontSize: 12
                                },
                                detail: {
                                    fontSize: 12,
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
