# vue

## Project setup
```
npm install
```

### Compiles and hot-reloads for development
```
npm run serve
```

### Compiles and minifies for production
```
npm run build
```

### Customize configuration
See [Configuration Reference](https://cli.vuejs.org/config/).

### 后端技术
- **框架**: Spring Boot 2.7.10
- **数据库**: MySQL 8.0
- **缓存**: Redis (用于抢课并发控制)
- **ORM框架**: MyBatis-Plus 3.5.1
- **安全认证**: JWT (java-jwt 3.10.3)

### 2. 课程管理 (CourseController)
- 课程信息CRUD
- 课程容量控制与抢课校验
- Redis 缓存预热与名额扣减
- 课程评价流管理（支持多级回复）
- 课程统计分析
