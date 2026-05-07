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
        // 1. 先检查是否已经选过该课程
        Integer count = courseMapper.countStudentCourse(courseId, studentId);
        if (count > 0) {
            throw new ServiceException(Constants.CODE_600, "您已经选过这门课，请勿重复点击");
        }

        // 2. 检查课程容量
        Course course = getById(courseId);
        if (course == null) {
            throw new ServiceException(Constants.CODE_600, "课程不存在");
        }
        
        // 如果 capacity 为 null，则视作不限量
        if (course.getCapacity() != null) {
            // 查询当前已选人数
            Integer enrolledCount = courseMapper.countStudentCourse(courseId, null); // 需要修改 mapper 支持只查 courseId
            if (enrolledCount >= course.getCapacity()) {
                throw new ServiceException(Constants.CODE_600, "课程人数已满，抢课失败");
            }
        }

        courseMapper.setStudentCourse(courseId, studentId);
    }

    @Transactional
    public Page<User> getStudentList(Page<User> page, Integer courseId){
        return courseMapper.findStudentPage(page,courseId);
    }

    public List<Course> getCourseList (Integer studentId){
        return courseMapper.getCourseListByStudentId(studentId);
    }

    public void removeStudentCourseRecord(Integer id){
        courseMapper.deleteRecordById(id);
    }
}
