package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.Constants;
import com.example.springboot.entity.Course;
import com.example.springboot.entity.User;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.CourseMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

@Service
public class CourseService extends ServiceImpl<CourseMapper, Course> {

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String COURSE_STOCK_KEY = "course:stock:";
    private static final String COURSE_STUDENT_KEY = "course:students:";

    /**
     * 项目启动时，将课程容量同步到 Redis（数据预热）
     */
    @PostConstruct
    public void init() {
        List<Course> list = list();
        for (Course course : list) {
            if (course.getCapacity() != null) {
                // Redis 存储当前可用名额：容量 - 已选人数
                int stock = course.getCapacity() - (course.getEnrolled() == null ? 0 : course.getEnrolled());
                stringRedisTemplate.opsForValue().set(COURSE_STOCK_KEY + course.getId(), String.valueOf(stock));
            }
        }
    }

    public Page<Course> findPage(Page<Course> page, String name) {
        return courseMapper.findPage(page, name);
    }

    @Transactional
    public void setStudentCourse(Integer courseId, Integer studentId) {
        // 1. Redis 查重：检查该学生是否已经选过该课程（性能极高）
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(COURSE_STUDENT_KEY + courseId, studentId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            throw new ServiceException(Constants.CODE_600, "您已经选过这门课，请勿重复点击");
        }

        // 2. Redis 预扣名额（原子性控制，秒级拦截）
        String stockKey = COURSE_STOCK_KEY + courseId;
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        
        if (stockStr != null) { // 如果设置了容量限制
            long stock = stringRedisTemplate.opsForValue().decrement(stockKey);
            if (stock < 0) {
                // 恢复库存（防止负数）
                stringRedisTemplate.opsForValue().increment(stockKey);
                throw new ServiceException(Constants.CODE_600, "课程人数已满，抢课失败");
            }
        }

        // 3. Redis 校验通过后，再操作数据库（此时压力已经通过 Redis 过滤掉了 99%）
        // 这里仍然保留数据库查重作为最后一道防线
        Integer count = courseMapper.countStudentCourse(courseId, studentId);
        if (count > 0) {
            // 如果数据库里已经有了（可能 Redis 还没同步），回滚 Redis 库存
            if (stockStr != null) stringRedisTemplate.opsForValue().increment(stockKey);
            throw new ServiceException(Constants.CODE_600, "您已经选过这门课，请勿重复点击");
        }

        // 4. 原子性占用数据库名额
        int rows = courseMapper.incrementEnrolled(courseId);
        if (rows == 0) {
            // 如果数据库更新失败（可能 capacity 被管理员临时改小了），回滚 Redis 库存
            if (stockStr != null) stringRedisTemplate.opsForValue().increment(stockKey);
            throw new ServiceException(Constants.CODE_600, "课程人数已满，抢课失败");
        }

        // 5. 记录选课关系并同步到 Redis Set
        courseMapper.setStudentCourse(courseId, studentId);
        stringRedisTemplate.opsForSet().add(COURSE_STUDENT_KEY + courseId, studentId.toString());
    }

    @Transactional
    public Page<User> getStudentList(Page<User> page, Integer courseId){
        return courseMapper.findStudentPage(page,courseId);
    }

    public List<Course> getCourseList (Integer studentId){
        return courseMapper.getCourseListByStudentId(studentId);
    }

    @Transactional
    public void removeStudentCourseRecord(Integer id){
        // 1. 根据关联记录 ID 找到对应的课程 ID
        Course course = courseMapper.selectCourseByScId(id);
        if (course != null) {
            // 2. 原子性释放数据库名额
            courseMapper.decrementEnrolled(course.getId());
            
            // 3. 释放 Redis 名额并移除学生标记
            stringRedisTemplate.opsForValue().increment(COURSE_STOCK_KEY + course.getId());
            
            // 找到选课的学生ID并从 Redis Set 中移除
            // 注意：这里需要获取学生ID，可以通过关联表查出
            Integer studentId = courseMapper.selectStudentIdByScId(id);
            if (studentId != null) {
                stringRedisTemplate.opsForSet().remove(COURSE_STUDENT_KEY + course.getId(), studentId.toString());
            }
        }
        // 4. 删除数据库选课记录
        courseMapper.deleteRecordById(id);
    }
}
