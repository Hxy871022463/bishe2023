package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.Course;
import com.example.springboot.entity.User;
import com.example.springboot.mapper.CourseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.jws.soap.SOAPBinding;
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
        courseMapper.deleteStudentCourse(courseId, studentId);
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
