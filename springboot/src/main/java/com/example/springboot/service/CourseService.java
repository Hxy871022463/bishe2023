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

    @Resource
    private UserService userService;

    private static final String COURSE_STOCK_KEY = "course:stock:";
    private static final String COURSE_STUDENT_KEY = "course:students:";
    private static final String STUDENT_SCORE_KEY = "student:score:";

    /**
     * 项目启动时，将课程容量及学生已选学分同步到 Redis（数据预热）
     */
    @PostConstruct
    public void init() {
        // 1. 预热课程库存
        List<Course> list = list();
        for (Course course : list) {
            if (course.getCapacity() != null) {
                int stock = course.getCapacity() - (course.getEnrolled() == null ? 0 : course.getEnrolled());
                stringRedisTemplate.opsForValue().set(COURSE_STOCK_KEY + course.getId(), String.valueOf(stock));
            }
        }

        // 2. 预热学生已选总学分
        List<User> students = userService.list();
        for (User student : students) {
            Integer totalScore = courseMapper.sumScoreByStudentId(student.getId());
            stringRedisTemplate.opsForValue().set(STUDENT_SCORE_KEY + student.getId(), String.valueOf(totalScore == null ? 0 : totalScore));
        }
    }

    public Page<Course> findPage(Page<Course> page, String name) {
        return courseMapper.findPage(page, name);
    }

    /**
     * 新增或更新课程，并同步 Redis 缓存
     */
    @Transactional
    public void saveCourse(Course course) {
        saveOrUpdate(course);
        // 更新 Redis 中的名额库存
        if (course.getCapacity() != null) {
            // 重新从数据库获取最新的已选人数（确保数据准确）
            Course latestCourse = getById(course.getId());
            int enrolled = latestCourse.getEnrolled() == null ? 0 : latestCourse.getEnrolled();
            int stock = course.getCapacity() - enrolled;
            stringRedisTemplate.opsForValue().set(COURSE_STOCK_KEY + course.getId(), String.valueOf(stock));
        } else {
            // 如果删除了容量限制，移除 Redis 中的库存 Key
            stringRedisTemplate.delete(COURSE_STOCK_KEY + course.getId());
        }
    }

    @Transactional
    public void deleteCourseBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteCourse(id);
        }
    }

    @Transactional
    public void deleteCourse(Integer id) {
        removeById(id);
        // 清理 Redis 缓存
        stringRedisTemplate.delete(COURSE_STOCK_KEY + id);
        stringRedisTemplate.delete(COURSE_STUDENT_KEY + id);
    }

    @Transactional
    public void setStudentCourse(Integer courseId, Integer studentId) {
        // 1. Redis 查重
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(COURSE_STUDENT_KEY + courseId, studentId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            throw new ServiceException(Constants.CODE_600, "您已经选过这门课，请勿重复点击");
        }

        // 2. 学分限制校验 (核心新增逻辑)
        Course course = getById(courseId);
        if (course == null) {
            throw new ServiceException(Constants.CODE_600, "课程不存在");
        }
        User student = userService.getById(studentId);
        if (student == null) {
            throw new ServiceException(Constants.CODE_600, "学生不存在");
        }

        // 如果设置了最高学分限制
        if (student.getMaxScore() != null) {
            String scoreKey = STUDENT_SCORE_KEY + studentId;
            String currentScoreStr = stringRedisTemplate.opsForValue().get(scoreKey);
            int currentTotalScore = currentScoreStr == null ? 0 : Integer.parseInt(currentScoreStr);
            int courseScore = course.getScore() == null ? 0 : course.getScore();

            if (currentTotalScore + courseScore > student.getMaxScore()) {
                throw new ServiceException(Constants.CODE_600, "学分已达上限（最高 " + student.getMaxScore() + " 分），选课失败");
            }
        }

        // 3. Redis 预扣名额
        String stockKey = COURSE_STOCK_KEY + courseId;
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        
        if (stockStr != null) { 
            long stock = stringRedisTemplate.opsForValue().decrement(stockKey);
            if (stock < 0) {
                stringRedisTemplate.opsForValue().increment(stockKey);
                throw new ServiceException(Constants.CODE_600, "课程人数已满，抢课失败");
            }
        }

        // 4. 原子性占用数据库名额并插入记录
        int rows = courseMapper.incrementEnrolled(courseId);
        if (rows == 0) {
            if (stockStr != null) stringRedisTemplate.opsForValue().increment(stockKey);
            throw new ServiceException(Constants.CODE_600, "课程人数已满，抢课失败");
        }

        courseMapper.setStudentCourse(courseId, studentId);
        
        // 5. 同步更新 Redis 中的学生已选学分和学生名单
        stringRedisTemplate.opsForSet().add(COURSE_STUDENT_KEY + courseId, studentId.toString());
        if (course.getScore() != null) {
            stringRedisTemplate.opsForValue().increment(STUDENT_SCORE_KEY + studentId, course.getScore());
        }
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
        Integer studentId = courseMapper.selectStudentIdByScId(id);

        if (course != null && studentId != null) {
            // 2. 原子性释放数据库名额
            courseMapper.decrementEnrolled(course.getId());
            
            // 3. 释放 Redis 名额
            stringRedisTemplate.opsForValue().increment(COURSE_STOCK_KEY + course.getId());
            
            // 4. 回收学生学分并移除学生标记
            if (course.getScore() != null) {
                stringRedisTemplate.opsForValue().decrement(STUDENT_SCORE_KEY + studentId, course.getScore());
            }
            stringRedisTemplate.opsForSet().remove(COURSE_STUDENT_KEY + course.getId(), studentId.toString());
        }
        // 5. 删除数据库选课记录
        courseMapper.deleteRecordById(id);
    }

    public java.util.Map<String, Object> getStudentCreditInfo(Integer studentId) {
        User student = userService.getById(studentId);
        String scoreKey = STUDENT_SCORE_KEY + studentId;
        String currentScoreStr = stringRedisTemplate.opsForValue().get(scoreKey);
        int currentScore = currentScoreStr == null ? 0 : Integer.parseInt(currentScoreStr);
        Integer maxScore = student == null ? null : student.getMaxScore();
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("currentScore", currentScore);
        result.put("maxScore", maxScore);
        return result;
    }
}
