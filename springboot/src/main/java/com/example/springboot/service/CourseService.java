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
        // 先检查是否已经选过该课程
        Integer count = courseMapper.countStudentCourse(courseId, studentId);
        if (count > 0) {
            throw new ServiceException(Constants.CODE_600, "您已经选过这门课，请勿重复点击");
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
