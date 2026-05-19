package com.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Course;
import com.example.springboot.entity.User;
import com.example.springboot.service.CourseService;
import com.example.springboot.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/course")
public class CourseController {

    @Resource
    private CourseService courseService;

    @Resource
    private UserService userService;

    // 新增或者更新
    @PostMapping
    public Result save(@RequestBody Course course) {
        courseService.saveCourse(course);
        return Result.success();
    }

    @PostMapping("/studentCourse/{courseId}/{studentId}")
    public Result studentCourse(@PathVariable Integer courseId, @PathVariable Integer studentId) {
        courseService.setStudentCourse(courseId, studentId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        courseService.deleteCourse(id);
        return Result.success();
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        courseService.deleteCourseBatch(ids);
        return Result.success();
    }

    @GetMapping
    public Result findAll() {
        return Result.success(courseService.list());
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(courseService.getById(id));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam String name,
                           @RequestParam Integer pageNum,
                           @RequestParam Integer pageSize) {
//        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();
//        queryWrapper.orderByDesc("id");
//        Page<Course> page = courseService.page(new Page<>(pageNum, pageSize), queryWrapper);
//        List<Course> records = page.getRecords();
//        for (Course record : records) {
//            User user = userService.getById(record.getTeacherId());
//            if(user != null) {
//                record.setTeacher(user.getNickname());
//            }
//
//        }
        Page<Course> page = courseService.findPage(new Page<>(pageNum, pageSize), name);
        return Result.success(page);
    }

    @GetMapping("/studentList")
    public Result getStudentList(@RequestParam Integer id,@RequestParam Integer pageNum, @RequestParam Integer pageSize){
        Page<User> page = courseService.getStudentList(new Page<>(pageNum, pageSize), id);
        return Result.success(page);
    }

    @GetMapping("/student/courseList/{id}")
    public Result getStudentCourseList(@PathVariable Integer id){
        return Result.success(courseService.getCourseList(id));
    }

    @GetMapping("/student/exitCourse/{id}")
    public Result exitCourse(@PathVariable Integer id){
        courseService.removeStudentCourseRecord(id);
        return Result.success();
    }

    @GetMapping("/student/creditInfo/{id}")
    public Result getCreditInfo(@PathVariable Integer id){
        return Result.success(courseService.getStudentCreditInfo(id));
    }
}
