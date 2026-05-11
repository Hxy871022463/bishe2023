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
1. 用户在前端输入用户名和密码。
2. 后端接收请求，对密码进行 MD5 加密处理，与数据库比对。
3. 验证通过后，后端根据用户 ID 和密码生成 JWT Token。
4. 后端根据用户角色，查询并过滤对应的菜单树（Menu Tree）。
5. 后端返回包含 Token 和菜单信息的 `UserDTO` 对象。
6. 前端将 Token 存储在 localStorage，并在后续请求的 Header 中携带。

**数据流向：**
`前端请求 (POST /user/login)` -> `UserController` -> `UserService (MD5校验)` -> `TokenUtils (生成Token)` -> `UserService (获取并过滤菜单)` -> `返回 UserDTO` -> `前端存储并渲染侧边栏`。

### **2.2 基于角色的访问控制 (RBAC)**
**核心区分逻辑：**
系统通过 `sys_user` 表中的 `role` 字段区分身份。在后端，使用 [RoleEnum.java](file:///D:/bishe/bishe2023/springboot/src/main/java/com/example/springboot/common/RoleEnum.java) 规范了四种角色：`ROLE_ADMIN` (管理员), `ROLE_TEACHER` (老师), `ROLE_STUDENT` (学生), `ROLE_USER` (普通用户)。

**业务流程：**
1. **菜单与权限分配**: 管理员在后台为不同角色勾选可访问的菜单。
2. **自动补全机制**: 在 `RoleService.setRoleMenu` 中，若分配了二级菜单，系统会自动补全父级一级菜单，确保菜单树的完整性。
3. **动态渲染**: 用户登录后，后端根据其角色标识从 `sys_role_menu` 表过滤出对应的菜单，前端据此生成导航栏。

### **2.3 选课业务逻辑与抢课控制 (Course Selection & Competition)**
**业务流程：**
1. 教师发布课程信息，并设定 **课程容量 (Capacity)**。
2. 系统启动时执行 **数据预热 (Warm-up)**，将课程余量同步至 Redis。
3. 学生点击“选课”，触发 `CourseController.studentCourse` 接口。
4. **Redis 拦截层**：
    - 利用 Redis Set 进行极速 **查重校验**。
    - 利用 Redis `decrement` 原子操作进行 **预扣减库存**，实现秒级拦截。
5. **数据库持久层**：
    - 校验通过后，执行 MySQL **原子更新** (`enrolled = enrolled + 1`)。
    - 在 `student_course` 关联表中插入记录。
6. 若选课成功，前端自动刷新列表，展示最新的“已选/容量”进度。

### **2.4 文件上传与秒传逻辑 (File Management)**
**业务流程：**
1. 后端计算文件 MD5 值，若数据库已存在相同 MD5 则直接返回现有 URL（实现秒传）。
2. 若不存在，则保存至本地磁盘并将元数据存入 `sys_file` 表。

### **2.5 课程评价系统 (Course Evaluation)**
**业务流程：**
1. 学生选课并修完课程后，可对课程进行星级评分 (1-5星) 和文字评价。
2. 系统支持多级回复（如老师回复学生评价），并实现内联展示。
3. **数据安全性**: 后端强制通过 JWT Token 解析出当前登录用户的真实 ID，防止伪造 `userId` 提交请求。
4. **业务健壮性**: 在 Service 层增加校验逻辑，同一学生对同一门课程仅允许发表一条主评价，防止刷分。

---

## **3. 核心技术细节说明**

### **3.1 Redis 高并发优化方案**
- **数据一致性**: 采用“冗余字段 + 原子更新”策略，`enrolled` 字段持久化至 `course` 表。
- **流量削峰**: 通过 Redis 预扣减名额，保护 MySQL 数据库不被瞬间流量冲垮。
- **原子性保证**: 利用数据库行锁与 Redis 单线程特性，彻底杜绝“超卖”现象。

### **3.2 JWT 无状态认证与安全性**
- **防篡改**: 使用用户密码作为签名密钥，确保 Token 不可伪造。
- **身份溯源**: 通过 `TokenUtils.getCurrentUser()` 从请求头中解析出真实的 `User` 对象，这是后端权限验证的唯一标准。

### **3.3 全局异常处理 (Global Exception Handling)**
- 统一捕获 `ServiceException`，将业务逻辑错误（如“您已评价过该课”、“人数已满”）以 `600` 等状态码返回前端，提升系统健壮性。

### **3.3 数据库设计最佳实践**
- **字符集**: 采用 `utf8mb4` 以支持评论中的 Emoji 表情。
- **性能**: 在 `sys_comment` 表的 `course_id` 和 `origin_id` 上建立索引，加速课程评价流的查询。

---

## **4. 关键接口定义参考**

| 功能模块 | 接口路径 | 请求方式 | 说明 |
| :--- | :--- | :--- | :--- |
| 用户登录 | `/user/login` | POST | 返回 Token 与 权限菜单 |
| 角色菜单设置 | `/role/roleMenu/{id}` | POST | 动态分配角色权限 |
| 课程选修 | `/course/studentCourse/{courseId}/{studentId}` | POST | 学生选课核心接口 |
| 课程评价 | `/comment` | POST | [设计中] 提交评分与评论 |

---

## **5. 数据库核心表设计**

- **sys_user**: 用户表，含 `role` 字段。
- **sys_role / sys_menu / sys_role_menu**: RBAC 权限体系三剑客。
- **course / student_course**: 课程及选课关联表。
- **sys_comment**: [新增] 存储评价内容、评分、父级 ID 及根评论 ID。

---

## **6. SQL 扩展参考 (课程评价表)**

```sql
CREATE TABLE `sys_comment` (
  `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `content` varchar(1000) COMMENT '评价内容',
  `user_id` int(11) COMMENT '评价人ID',
  `course_id` int(11) COMMENT '课程ID',
  `rate` int(1) COMMENT '评分(1-5星)',
  `time` datetime DEFAULT CURRENT_TIMESTAMP,
  `parent_id` int(11) COMMENT '父级评论ID',
  `origin_id` int(11) COMMENT '根评论ID',
  INDEX `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
