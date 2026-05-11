package com.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.Course;
import com.example.springboot.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseMapper extends BaseMapper<Course> {

    Page<Course> findPage(Page<Course> page, @Param("name") String name);

    void deleteStudentCourse(@Param("courseId")Integer courseId, @Param("studentId") Integer studentId);

    void setStudentCourse(@Param("courseId")Integer courseId, @Param("studentId") Integer studentId);

    Integer countStudentCourse(@Param("courseId")Integer courseId, @Param("studentId") Integer studentId);

    Page<User> findStudentPage(Page<User> page, @Param("courseId") Integer id);

    List<Course> getCourseListByStudentId(@Param("studentId")Integer studentId);

    void deleteRecordById(@Param("id")Integer id);

    Course selectCourseByScId(@Param("scId") Integer scId);

    Integer selectStudentIdByScId(@Param("scId") Integer scId);

    Integer sumScoreByStudentId(@Param("studentId") Integer studentId);

    int incrementEnrolled(@Param("courseId") Integer courseId);

    int decrementEnrolled(@Param("courseId") Integer courseId);
}
