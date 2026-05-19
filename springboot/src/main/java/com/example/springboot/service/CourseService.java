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


    // 缓存键常量（建议在类顶部定义或引入）
    // COURSE_STOCK_KEY: 课程库存 (String类型)
    // STUDENT_SCORE_KEY: 学生当前已选总学分 (String类型)
    // COURSE_STUDENT_KEY: 课程已选学生集合 (Set类型，用于防重)

    /**
     * 系统启动预热：将数据库中的关键数据同步至 Redis 缓存。
     * 避免高并发抢课初期出现“缓存穿透”与“缓存击穿”。
     */
    @PostConstruct
    public void init() {
        List<Course> list = list();
        for (Course course : list) {
            if (course.getCapacity() != null) {
                int stock = course.getCapacity() - (course.getEnrolled() == null ? 0 : course.getEnrolled());
                stringRedisTemplate.opsForValue().set(COURSE_STOCK_KEY + course.getId(), String.valueOf(stock));
            }
        }
        List<User> students = userService.list();
        for (User student : students) {
            Integer totalScore = courseMapper.sumScoreByStudentId(student.getId());
            stringRedisTemplate.opsForValue().set(STUDENT_SCORE_KEY + student.getId(), String.valueOf(totalScore == null ? 0 : totalScore));
        }
    }

    /**
     * 新增或更新课程信息
     * 采用“先写数据库，再写缓存”的策略，保证管理端修改后，抢课端能实时感知库存变化。
     */
    @Transactional
    public void saveCourse(Course course) {
        saveOrUpdate(course);
        if (course.getCapacity() != null) {
            Course latestCourse = getById(course.getId());
            int enrolled = latestCourse.getEnrolled() == null ? 0 : latestCourse.getEnrolled();
            int stock = course.getCapacity() - enrolled;
            stringRedisTemplate.opsForValue().set(COURSE_STOCK_KEY + course.getId(), String.valueOf(stock));
        } else {
            stringRedisTemplate.delete(COURSE_STOCK_KEY + course.getId());
        }
    }

    // 批量删除课程
    @Transactional
    public void deleteCourseBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteCourse(id);
        }
    }

    /**
     * 删除单门课程
     * 级联删除数据库记录与对应的 Redis 缓存（库存缓存与学生抢课防重 Set）
     */
    @Transactional
    public void deleteCourse(Integer id) {
        removeById(id);
        stringRedisTemplate.delete(COURSE_STOCK_KEY + id);
        stringRedisTemplate.delete(COURSE_STUDENT_KEY + id);
    }

    /**
     * 分页查询课程列表（直接走数据库，适用于非核心高并发的管理端或列表页）
     */
    public Page<Course> findPage(Page<Course> page, String name) {
        return courseMapper.findPage(page, name);
    }

    /**
     * 核心业务：高并发抢课/选课逻辑
     * 核心拦截顺序：1.Redis防重 -> 2.基础校验 -> 3.Redis学分校验 -> 4.Redis预减库存 -> 5.DB乐观锁更新
     *
     * @param courseId 课程ID
     * @param studentId 学生ID
     */
    @Transactional
    public void setStudentCourse(Integer courseId, Integer studentId) {
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(COURSE_STUDENT_KEY + courseId, studentId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            throw new ServiceException(Constants.CODE_600, "您已经选过这门课，请勿重复点击");
        }

        Course course = getById(courseId);
        if (course == null) {
            throw new ServiceException(Constants.CODE_600, "课程不存在");
        }
        User student = userService.getById(studentId);
        if (student == null) {
            throw new ServiceException(Constants.CODE_600, "学生不存在");
        }

        if (student.getMaxScore() != null) {
            String scoreKey = STUDENT_SCORE_KEY + studentId;
            String currentScoreStr = stringRedisTemplate.opsForValue().get(scoreKey);
            int currentTotalScore = currentScoreStr == null ? 0 : Integer.parseInt(currentScoreStr);
            int courseScore = course.getScore() == null ? 0 : course.getScore();

            if (currentTotalScore + courseScore > student.getMaxScore()) {
                throw new ServiceException(Constants.CODE_600, "学分已达上限（最高 " + student.getMaxScore() + " 分），选课失败");
            }
        }

        String stockKey = COURSE_STOCK_KEY + courseId;
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        
        if (stockStr != null) { 
            long stock = stringRedisTemplate.opsForValue().decrement(stockKey);
            if (stock < 0) {
                stringRedisTemplate.opsForValue().increment(stockKey);
                throw new ServiceException(Constants.CODE_600, "课程人数已满，抢课失败");
            }
        }

        int rows = courseMapper.incrementEnrolled(courseId);
        if (rows == 0) {
            if (stockStr != null) stringRedisTemplate.opsForValue().increment(stockKey);
            throw new ServiceException(Constants.CODE_600, "课程人数已满，抢课失败");
        }

        courseMapper.setStudentCourse(courseId, studentId);
        
        stringRedisTemplate.opsForSet().add(COURSE_STUDENT_KEY + courseId, studentId.toString());
        if (course.getScore() != null) {
            stringRedisTemplate.opsForValue().increment(STUDENT_SCORE_KEY + studentId, course.getScore());
        }
    }

    /**
     * 查询某门课程的学生列表（分页）
     */
    @Transactional
    public Page<User> getStudentList(Page<User> page, Integer courseId){
        return courseMapper.findStudentPage(page,courseId);
    }

    /**
     * 获取某个学生已选的课程列表
     */
    public List<Course> getCourseList (Integer studentId){
        return courseMapper.getCourseListByStudentId(studentId);
    }

    /**
     * 取消选课 / 退课逻辑
     * 与抢课相反，属于逆向操作：扣减数据库已选人数 -> 归还 Redis 库存 -> 扣减 Redis 学分 -> 移出防重集合 -> 删除关系记录
     * * @param id 选课关系表的主键ID
     */
    @Transactional
    public void removeStudentCourseRecord(Integer id){
        Course course = courseMapper.selectCourseByScId(id);
        Integer studentId = courseMapper.selectStudentIdByScId(id);

        if (course != null && studentId != null) {
            courseMapper.decrementEnrolled(course.getId());
            stringRedisTemplate.opsForValue().increment(COURSE_STOCK_KEY + course.getId());
            if (course.getScore() != null) {
                stringRedisTemplate.opsForValue().decrement(STUDENT_SCORE_KEY + studentId, course.getScore());
            }
            stringRedisTemplate.opsForSet().remove(COURSE_STUDENT_KEY + course.getId(), studentId.toString());
        }
        courseMapper.deleteRecordById(id);
    }

    /**
     * 获取学生当前的学分信息（优先从 Redis 缓存获取当前学分，结合 DB 获取最大允许学分）
     */
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
