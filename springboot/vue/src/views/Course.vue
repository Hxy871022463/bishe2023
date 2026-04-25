<template>
<div>
    <div style="margin: 10px 0">
        <el-input style="width: 200px" placeholder="请输入名称" suffix-icon="el-icon-search" v-model="name"></el-input>
        <el-button class="ml-5" type="primary" @click="load">搜索</el-button>
        <el-button type="warning" @click="reset">重置</el-button>
    </div>
    <div style="margin: 10px 0">
        <el-button type="primary" @click="handleAdd" v-if="user.role === 'ROLE_ADMIN'">新增 <i class="el-icon-circle-plus-outline"></i></el-button>
        <el-popconfirm
                class="ml-5"
                confirm-button-text='确定'
                cancel-button-text='我再想想'
                icon="el-icon-info"
                icon-color="red"
                title="您确定批量删除这些数据吗？"
                @confirm="delBatch"
        >
            <el-button type="danger" slot="reference" v-if="user.role==='ROLE_ADMIN'">批量删除 <i class="el-icon-remove-outline"></i></el-button>
        </el-popconfirm>
        <el-button type="success" v-if="user.role==='ROLE_STUDENT'" @click="getSelectedCourseList">查看当前选课信息</el-button>
    </div>
    <el-table :data="tableData" border stripe :header-cell-class-name="'headerBg'"
              @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="name" label="课程名称"></el-table-column>
        <el-table-column prop="score" label="学分"></el-table-column>
        <el-table-column prop="times" label="课时"></el-table-column>
        <el-table-column prop="teacher" label="授课老师"></el-table-column>
        <el-table-column label="启用" v-if="user.role === 'ROLE_ADMIN'">
            <template slot-scope="scope">
                <el-switch v-model="scope.row.state" active-color="#13ce66" inactive-color="#ccc"
                           @change="changeEnable(scope.row)"></el-switch>
            </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center">
            <template slot-scope="scope">
                <el-button type="primary" @click="selectCourse(scope.row.id)" v-if="user.role === 'ROLE_STUDENT' ">选课</el-button>
                <el-button type="primary" @click="handleShowStudent(scope.row.id) " v-if="user.role === 'ROLE_TEACHER' ">查看选课学生</el-button>
                <el-button type="success" @click="handleEdit(scope.row)" v-if="user.role === 'ROLE_ADMIN'">编辑 <i class="el-icon-edit"></i></el-button>
                <el-popconfirm
                        class="ml-5"
                        confirm-button-text='确定'
                        cancel-button-text='我再想想'
                        icon="el-icon-info"
                        icon-color="red"
                        title="您确定删除吗？"
                        @confirm="del(scope.row.id)"
                >
                    <el-button type="danger" slot="reference" v-if="user.role === 'ROLE_ADMIN'">删除 <i class="el-icon-remove-outline"></i></el-button>
                </el-popconfirm>
            </template>
        </el-table-column>
    </el-table>

    <div style="padding: 10px 0">
        <el-pagination
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                :current-page="pageNum"
                :page-sizes="[2, 5, 10, 20]"
                :page-size="pageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="total">
        </el-pagination>
    </div>

    <el-dialog title="已选课信息" :visible.sync="courseListVisible" width="50%">
        <el-table
                :data="studentCourses"
                style="width: 100%">
            <el-table-column
                    prop="name"
                    label="课程名"
                    width="180">
            </el-table-column>
            <el-table-column
                    prop="score"
                    label="学分"
                    width="180">
            </el-table-column>
            <el-table-column
                    prop="times"
                    label="学时">
            </el-table-column>
            <el-table-column
                    prop="teacher"
                    label="教师名">
            </el-table-column>
            <el-table-column>
                <template slot-scope="scope">
                    <el-button type="danger" @click="Withdrawal(scope.row.scId)">退课</el-button>
                </template>
            </el-table-column>
        </el-table>
    </el-dialog>

    <el-dialog title="选课学生信息" :visible.sync="studentListVisible" width="50%">
        <el-table
                :data="students"
                style="width: 100%">
            <el-table-column
                    prop="username"
                    label="用户名"
                    width="180">
            </el-table-column>
            <el-table-column
                    prop="nickname"
                    label="姓名"
                    width="180">
            </el-table-column>
            <el-table-column
                    prop="email"
                    label="邮箱">
            </el-table-column>
            <el-table-column
                    prop="phone"
                    label="电话">
            </el-table-column>

        </el-table>
        <el-pagination
                @size-change="handleStudentSizeChange"
                @current-change="handleStudentCurrentChange"
                :current-page="this.studentPageNum"
                :page-sizes="[10, 20, 50]"
                :page-size="this.studentPageSize"
                layout="total, sizes, prev, pager, next"
                :total="this.studentTotal">
        </el-pagination>
    </el-dialog>
    <el-dialog title="课程信息" :visible.sync="dialogFormVisible" width="30%" >
        <el-form label-width="80px" size="small">
            <el-form-item label="名称">
                <el-input v-model="form.name" autocomplete="off"></el-input>
            </el-form-item>
            <el-form-item label="学分">
                <el-input v-model="form.score" autocomplete="off"></el-input>
            </el-form-item>
            <el-form-item label="课时">
                <el-input v-model="form.times" autocomplete="off"></el-input>
            </el-form-item>
            <el-form-item label="老师">
                <el-select clearable v-model="form.teacherId" placeholder="请选择">
                    <el-option v-for="item in teachers" :key="item.id" :label="item.nickname" :value="item.id"></el-option>
                </el-select>
            </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
            <el-button @click="dialogFormVisible = false">取 消</el-button>
            <el-button type="primary" @click="save">确 定</el-button>
        </div>
    </el-dialog>
</div>
</template>

<script>
export default {
    name: "Course",
    data() {
        return {
            form: {},
            tableData: [],
            name: '',
            multipleSelection: [],
            pageNum: 1,
            pageSize: 10,
            total: 0,
            studentPageNum: 1,
            studentPageSize: 10,
            studentTotal: 0,
            dialogFormVisible: false,
            studentListVisible: false,
            courseListVisible:false,
            teachers: [],
            students: [],
            studentCourses: [],
            selectId: 0,
            user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {}
        }
    },
    created() {
        this.load()
    },
    methods: {
        selectCourse(courseId) {
            this.request.post('/course/studentCourse/' + courseId + "/" + this.user.id).then(res => {
                if (res.code === '200') {
                    this.$message.success("选课成功")
                } else {
                    this.$message.success(res.msg)
                }
            })
        },
        load() {
            this.request.get("/course/page", {
                params: {
                    pageNum: this.pageNum,
                    pageSize: this.pageSize,
                    name: this.name,
                }
            }).then(res => {
                this.tableData = res.data.records
                this.total = res.data.total
            })
            this.request.get("/user/role/ROLE_TEACHER").then(res => {
                this.teachers = res.data
            })
        },
        changeEnable(row) {
            this.request.post("/course/update", row).then(res => {
                if (res.code === '200') {
                    this.$message.success("操作成功")
                }
            })
        },
        handleAdd() {
            this.dialogFormVisible = true
            this.form = {}
        },
        handleEdit(row) {
            this.form = JSON.parse(JSON.stringify(row))
            this.dialogFormVisible = true
        },
        del(id) {
            this.request.delete("/course/" + id).then(res => {
                if (res.code === '200') {
                    this.$message.success("删除成功")
                    this.load()
                } else {
                    this.$message.error("删除失败")
                }
            })
        },
        handleSelectionChange(val) {
            console.log(val)
            this.multipleSelection = val
        },
        delBatch() {
            let ids = this.multipleSelection.map(v => v.id)  // [{}, {}, {}] => [1,2,3]
            this.request.post("/course/del/batch", ids).then(res => {
                if (res.code === '200') {
                    this.$message.success("批量删除成功")
                    this.load()
                } else {
                    this.$message.error("批量删除失败")
                }
            })
        },
        save() {
            this.request.post("/course", this.form).then(res => {
                if (res.code === '200') {
                    this.$message.success("保存成功")
                    this.dialogFormVisible = false
                    this.load()
                } else {
                    this.$message.error("保存失败")
                }
            })
        },
        reset() {
            this.name = ""
            this.load()
        },
        handleSizeChange(pageSize) {
            console.log(pageSize)
            this.pageSize = pageSize
            this.load()
        },
        handleCurrentChange(pageNum) {
            console.log(pageNum)
            this.pageNum = pageNum
            this.load()
        },
        download(url) {
            window.open(url)
        },
        getStudentList(){
            //传课程id
            this.request.get("/course/studentList", {
                params: {
                    pageNum: this.studentPageNum,
                    pageSize: this.studentPageSize,
                    id: this.selectId,
                }
            }).then(res => {
                if (res.code === '200') {
                    this.students = res.data.records
                    this.studentTotal = res.data.total
                } else {
                    this.$message.error("获取学生列表失败")
                }
            })
        },
        handleStudentSizeChange(pageSize) {
            this.studentPageSize = pageSize
            this.getStudentList()
        },
        handleStudentCurrentChange(pageNum) {
            this.studentPageNum = pageNum
            this.getStudentList()
        },
        handleShowStudent(val){
            this.selectId = val
            this.studentListVisible = true
            this.getStudentList()
        },
        getSelectedCourseList(){
            this.courseListVisible = true
            this.request.get("/course/student/courseList/"+this.user.id).then(res => {
                if (res.code === '200') {
                    this.studentCourses = res.data
                } else {
                    this.$message.error("获取已选课列表失败")
                }
            })
        },
        Withdrawal(id){
            this.request.get("/course/student/exitCourse/" + id).then(res => {
                if (res.code === '200') {
                    this.$message.success("退课成功")
                    this.getSelectedCourseList()
                } else {
                    this.$message.error("退课失败")
                }
            })
        }
    }
}
</script>

<style scoped>
</style>
