# 选课系统项目详细说明文档

本项目是一个基于 Spring Boot 和 Vue 的前后端分离选课管理系统。本文档旨在详细描述系统的业务流程、数据流向以及核心技术细节，帮助开发者快速理解系统架构与实现逻辑。

---

## **1. 系统架构概述**

系统采用经典的前后端分离架构：
- **前端 (Frontend)**: 基于 Vue.js + ElementUI 构建，负责页面渲染、路由跳转及状态管理。
- **后端 (Backend)**: 基于 Spring Boot 2.x 框架，集成 MyBatis-Plus 简化持久层开发，提供 RESTful 风格的 API。
- **数据库 (Database)**: MySQL 5.7+，存储用户信息、角色权限、课程数据及文件元数据。
- **认证授权 (Auth)**: 采用 JWT (JSON Web Token) 实现无状态认证，结合拦截器实现接口层面的权限校验。

---

## **2. 核心业务流程与数据流向**

### **2.1 用户认证与授权流程 (Login & Auth)**
**业务流程：**
1. 用户在前端输入用户名 and 密码。
2. 后端接收请求，对密码进行 MD5 加密处理，与数据库比对。
3. 验证通过后，后端根据用户 ID 和密码生成 JWT Token。
4. 后端根据用户角色，查询并过滤对应的菜单树（Menu Tree）。
5. 后端返回包含 Token 和菜单信息的 `UserDTO` 对象。
6. 前端将 Token 存储在 localStorage，并在后续请求的 Header 中携带。

### **2.2 基于角色的访问控制 (RBAC)**
**核心区分逻辑：**
系统通过 `sys_user` 表中的 `role` 字段区分身份。在后端，使用 [RoleEnum.java](file:///D:/bishe/bishe2023/springboot/src/main/java/com/example/springboot/common/RoleEnum.java) 规范了四种角色：`ROLE_ADMIN` (管理员), `ROLE_TEACHER` (老师), `ROLE_STUDENT` (学生), `ROLE_USER` (普通用户)。

### **2.3 选课业务逻辑 (Course Selection)**
**业务流程：**
1. 教师发布课程信息。
2. 学生登录系统，在 [Course.vue](file:///D:/bishe/bishe2023/springboot/vue/src/views/Course.vue) 查看可选课程列表。
3. 学生点击“选课”，触发 `CourseController.studentCourse` 接口。
4. `CourseService` 开启事务，在 `student_course` 关联表中插入记录。

### **2.4 课程评价系统 (Course Evaluation) [深度优化版]**
**业务流程：**
1. **发表评价**：已选课学生在“已选课程”弹窗中可点击“评价”按钮进行打分和文字评论。
2. **内联互动回复**：采用 **“内联回复框”** 设计，点击回复直接在当前评论下方弹出输入框，无需多层弹窗，操作体验流畅。
3. **评价折叠机制**：为了保持界面整洁，子评论超过 **2 条** 时会自动折叠。用户可点击“展开更多回复”查看完整互动。
4. **状态重置**：系统具备 **“默认收起”** 逻辑，每次重新打开评价列表时，所有折叠项均会自动恢复到初始收起状态。

---

## **3. 核心技术细节说明**

### **3.1 JWT 安全与身份溯源**
- **安全验证**：在 [CommentService.java](file:///D:/test/CommentService.java) 中，强制使用 `TokenUtils.getCurrentUser()` 解析 `userId`，确保评价者身份真实，防止接口恶意调用。
- **持久化策略**：登录状态持久化于 `LocalStorage`，通过路由守卫与 `setRoutes()` 机制实现刷新页面不丢失登录态。

### **3.2 业务健壮性与 UI 交互**
- **防刷机制**：后端 Service 层通过 `QueryWrapper` 确保同一学生对同一课程仅能发表一条主评论。
- **流式布局**：前端弃用传统的表格（Table）展现形式，改用 **Flex 弹性布局** 的信息流模式，配合头像与背景色区分，增强社交感。
- **动态折叠实现**：通过前端 `expandedCommentIds` 数组动态追踪展开状态，利用 `v-if` 与 `index` 索引实现轻量化的局部渲染控制。

### **3.3 数据库设计最佳实践**
- **字符集**: 全面采用 `utf8mb4`，完美支持 Emoji 表情符。
- **树形结构**: 利用 `parent_id` 建立数据库自关联，支撑无限级嵌套评论。
- **索引支撑**: 对 `course_id` 建立 B-Tree 索引，确保在大数据量下依然能秒开评论区。

---

## **4. 关键接口定义参考**

| 功能模块 | 接口路径 | 请求方式 | 说明 |
| :--- | :--- | :--- | :--- |
| 用户登录 | `/user/login` | POST | 返回 Token 与 动态权限菜单 |
| 课程选修 | `/course/studentCourse/{courseId}/{studentId}` | POST | 学生选课核心业务接口 |
| 评价/回复提交 | `/comment` | POST | 包含重复提交校验与 ParentID 绑定 |
| 评价树查询 | `/comment/tree/{courseId}` | GET | 一次性递归拉取所有层级的评价数据 |

---

## **5. 数据库核心表设计**

- **sys_user**: 用户表（含 ID、账号、密码、昵称、头像）。
- **sys_comment**: 课程评价表（含内容、评分、父级ID、根评论ID、时间）。
- **course / student_course**: 课程基本信息及选课关系映射。

---

## **6. 环境配置要求**

- **JDK**: 1.8 | **Maven**: 3.6+ | **MySQL**: 5.7+ | **Node.js**: 14.x+
