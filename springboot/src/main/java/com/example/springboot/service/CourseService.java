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

    @Transactional
    public void deleteCourseBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteCourse(id);
        }
    }

    @Transactional
    public void deleteCourse(Integer id) {
        removeById(id);
        stringRedisTemplate.delete(COURSE_STOCK_KEY + id);
        stringRedisTemplate.delete(COURSE_STUDENT_KEY + id);
    }

    public Page<Course> findPage(Page<Course> page, String name) {
        return courseMapper.findPage(page, name);
    }

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

    @Transactional
    public Page<User> getStudentList(Page<User> page, Integer courseId){
        return courseMapper.findStudentPage(page,courseId);
    }

    public List<Course> getCourseList (Integer studentId){
        return courseMapper.getCourseListByStudentId(studentId);
    }

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
