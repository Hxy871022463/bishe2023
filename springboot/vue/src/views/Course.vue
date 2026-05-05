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
                <el-button type="warning" @click="viewComments(scope.row.id)">查看评价</el-button>
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
            <el-table-column label="操作" width="180" align="center">
                <template slot-scope="scope">
                  <!-- 新增评价按钮 -->
                    <el-button type="primary" @click="handleComment(scope.row.id)">评价</el-button>
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
  <!-- 填写评价对话框 -->
    <el-dialog title="课程评价" :visible.sync="commentDialogVisible" width="30%">
      <el-form label-width="80px" size="small">
        <el-form-item label="评分" v-if="!commentForm.parentId">
          <el-rate v-model="commentForm.rate" style="margin-top: 10px"></el-rate>
        </el-form-item>
        <el-form-item label="内容">
          <el-input type="textarea" v-model="commentForm.content"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="commentDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitComment">确 定</el-button>
      </div>

      <!-- 查看评价对话框 -->
      <el-dialog title="课程评价列表" :visible.sync="commentListVisible" width="50%">
        <el-table :data="comments" border stripe>
          <el-table-column prop="nickname" label="评价人" width="120"></el-table-column>
          <el-table-column prop="rate" label="评分" width="150">
            <template slot-scope="scope">
              <el-rate v-model="scope.row.rate" disabled></el-rate>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容"></el-table-column>
          <el-table-column prop="time" label="评价时间" width="180"></el-table-column>
        </el-table>
      </el-dialog>
    </el-dialog>

  <!-- 查看评价列表对话框 -->
  <el-dialog title="课程评价列表" :visible.sync="commentListVisible" width="50%">
    <div style="max-height: 400px; overflow-y: auto; padding-right: 10px;"> <!-- 增加滚动条 -->
      <div v-for="item in comments" :key="item.id" style="border-bottom: 1px solid #eee; padding: 15px 0;">

        <!-- 一级评论布局 -->
        <div style="display: flex;">
          <div style="width: 50px;">
            <el-avatar :size="40" :src="item.avatarUrl"></el-avatar>
          </div>
          <div style="flex: 1; margin-left: 10px;">
            <div style="display: flex; justify-content: space-between;">
              <b style="color: #333">{{ item.nickname }}</b>
              <span style="color: #999; font-size: 12px">{{ item.time }}</span>
            </div>
            <div style="margin-top: 5px">
              <el-rate v-model="item.rate" disabled></el-rate>
            </div>
            <div style="margin-top: 10px; color: #444; line-height: 22px;">{{ item.content }}</div>
            <!-- 在“回复”按钮所在位置的下方添加 -->
            <div style="margin-top: 10px">
              <el-button type="text" size="mini" @click="handleReplyInline(item)">回复</el-button>

              <!-- 内联回复框：只有当 replyCommentId 等于当前评论 ID 时才显示 -->
              <div v-if="replyCommentId === item.id" style="margin-top: 10px; background: #fff; padding: 10px; border: 1px solid #dcdfe6; border-radius: 4px;">
                <el-input
                    type="textarea"
                    :rows="2"
                    placeholder="请输入回复内容..."
                    v-model="replyContent">
                </el-input>
                <div style="margin-top: 10px; text-align: right;">
                  <el-button size="mini" @click="replyCommentId = null">取消</el-button>
                  <el-button size="mini" type="primary" @click="doReply(item)">提交回复</el-button>
                </div>
              </div>
            </div>

            <!-- 子评论（回复）布局 -->
            <div v-if="item.children && item.children.length" style="margin-top: 15px; background-color: #f9f9f9; padding: 10px; border-radius: 5px;">

              <!-- 遍历子评论 -->
              <div v-for="(sub, index) in item.children" :key="sub.id">
                <!-- 逻辑：只显示前 2 条，或者当 ID 在展开列表中时显示全部 -->
                <div v-if="index < 2 || expandedCommentIds.includes(item.id)"
                     style="margin-bottom: 10px; border-bottom: 1px dashed #ddd; padding-bottom: 5px;">
                  <div style="display: flex; justify-content: space-between;">
                    <span style="color: #409EFF; font-weight: bold;">{{ sub.nickname }}：</span>
                    <span style="color: #999; font-size: 11px">{{ sub.time }}</span>
                  </div>
                  <div style="margin-top: 5px; color: #666;">{{ sub.content }}</div>
                </div>
              </div>

              <!-- 展开/折叠按钮：仅当子评论超过 2 条时显示 -->
              <div v-if="item.children.length > 2" style="text-align: center; margin-top: 5px;">
                <el-button type="text" size="mini" @click="toggleExpand(item.id)" style="color: #909399">
                  {{ expandedCommentIds.includes(item.id) ? '收起回复' : '展开更多回复 (' + item.children.length + '条)' }}
                  <i :class="expandedCommentIds.includes(item.id) ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
                </el-button>
              </div>

            </div>

          </div>
        </div>

      </div>
      <!-- 无评论时的占位提示 -->
      <div v-if="comments.length === 0" style="text-align: center; color: #999; padding: 20px;">
        暂无评价，快来抢沙发吧~
      </div>
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
            user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {},
            commentDialogVisible: false,
            commentListVisible: false,
            commentForm: {},
            comments: [],
            replyCommentId: null, // 记录当前点开了哪条评论的回复框
            replyContent: "",     // 临时存储回复框输入的文字
            expandedCommentIds: [], // 记录哪些父评论的子评论是展开状态
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
        },
      // 在 Course.vue 的 methods 中增加
      handleComment(courseId) {
        this.commentForm = { courseId: courseId, rate: 5, content: '' }
        this.commentDialogVisible = true
      },
      submitComment() {
        this.request.post("/comment", this.commentForm).then(res => {
          if (res.code === '200') {
            this.$message.success("评价成功")
            this.commentDialogVisible = false
          } else {
            this.$message.error(res.msg)
          }
        })
      },
      viewComments(courseId) {
        this.request.get("/comment/tree/" + courseId).then(res => {
          if (res.code === '200') {
            this.comments = res.data
            this.commentListVisible = true
          }
        })
      },
      // 在 viewComments 方法之后添加
      handleReply(row) {
        this.commentForm = {
          courseId: row.courseId,
          parentId: row.id,  // 绑定父级ID
          originId: row.originId || row.id, // 绑定根ID
          content: ''
        }
        this.commentDialogVisible = true // 弹出评论框
      },
      // 1. 点击“回复”按钮时触发：不再弹窗，而是显示内联输入框
      handleReplyInline(row) {
        this.replyCommentId = row.id // 标记当前要回复这一行
        this.replyContent = ""       // 清空之前可能写过的内容
      },

// 2. 执行真正的回复提交
      doReply(row) {
        if (!this.replyContent) {
          this.$message.warning("请输入回复内容")
          return
        }
        const data = {
          courseId: row.courseId,
          parentId: row.id,             // 绑定父级 ID
          originId: row.originId || row.id, // 绑定根 ID
          content: this.replyContent
        }
        this.request.post("/comment", data).then(res => {
          if (res.code === '200') {
            this.$message.success("回复成功")
            this.replyCommentId = null // 隐藏输入框
            this.replyContent = ""      // 清空文字
            this.viewComments(row.courseId) // 重新刷新列表，看到新回复
          } else {
            this.$message.error(res.msg)
          }
        })
      },
        toggleExpand(id) {
          const index = this.expandedCommentIds.indexOf(id);
          if (index > -1) {
          this.expandedCommentIds.splice(index, 1); // 如果已展开，则移除（折叠）
          } else {
          this.expandedCommentIds.push(id); // 如果未展开，则添加（展开）
        }
      },
    }
}
</script>

<style scoped>
</style>
