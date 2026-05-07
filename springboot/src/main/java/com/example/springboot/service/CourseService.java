package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.Constants;
import com.example.springboot.entity.Course;
import com.example.springboot.entity.User;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.CourseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CourseService extends ServiceImpl<CourseMapper, Course> {

    @Resource
    private CourseMapper courseMapper;

    public Page<Course> findPage(Page<Course> page, String name) {
        return courseMapper.findPage(page, name);
    }

    @Transactional
    public void setStudentCourse(Integer courseId, Integer studentId) {
        // 1. 先检查该学生是否已经选过该课程（业务查重）
        Integer count = courseMapper.countStudentCourse(courseId, studentId);
        if (count > 0) {
            throw new ServiceException(Constants.CODE_600, "您已经选过这门课，请勿重复点击");
        }

        // 2. 尝试原子性占用名额（性能优化 + 并发控制）
        // incrementEnrolled 会通过 SQL 语句: update course set enrolled = enrolled + 1 where id = ? and enrolled < capacity
        // 如果返回值为 1，说明名额占用成功；如果返回值为 0，说明名额已满或课程不存在。
        int rows = courseMapper.incrementEnrolled(courseId);
        if (rows == 0) {
            // 进一步判断是课程不存在还是名额已满
            Course course = getById(courseId);
            if (course == null) {
                throw new ServiceException(Constants.CODE_600, "课程不存在");
            }
            throw new ServiceException(Constants.CODE_600, "课程人数已满，抢课失败");
        }

        // 3. 记录选课关联关系
        courseMapper.setStudentCourse(courseId, studentId);
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
        // 1. 根据关联记录 ID 找到对应的课程 ID，以便后续减少名额
        Course course = courseMapper.selectCourseByScId(id);
        if (course != null) {
            // 2. 原子性释放名额
            courseMapper.decrementEnrolled(course.getId());
        }
        // 3. 删除选课记录
        courseMapper.deleteRecordById(id);
    }
}
