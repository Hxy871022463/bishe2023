package com.example.springboot.service;

import com.example.springboot.entity.Course;
import com.example.springboot.mapper.CourseMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    @Resource
    private CourseMapper courseMapper;

    public List<Course> getHotCourses() {
        return courseMapper.findHotCourses();
    }

    public Map<String, Object> getRateDistribution() {
        return getRateDistribution(null);
    }

    public Map<String, Object> getRateDistribution(Integer courseId) {
        List<Map<String, Object>> list = courseMapper.findRateDistribution(courseId);
        Map<String, Object> result = new HashMap<>();
        long total = 0;
        for (Map<String, Object> item : list) {
            Number rateNumber = (Number) item.get("rate");
            Number countNumber = (Number) item.get("count");
            Integer rate = rateNumber != null ? rateNumber.intValue() : 0;
            Integer count = countNumber != null ? countNumber.intValue() : 0;
            result.put(rate + "星", count);
            total += count;
        }
        result.put("total", total);
        return result;
    }

    public List<Map<String, Object>> getCourseSaturation() {
        List<Map<String, Object>> list = courseMapper.findCourseSaturation();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : list) {
            Map<String, Object> courseData = new HashMap<>();
            courseData.put("courseName", item.get("name"));
            courseData.put("teacher", item.get("teacher"));
            courseData.put("saturation", item.get("saturation"));
            courseData.put("enrolled", item.get("enrolled"));
            courseData.put("capacity", item.get("capacity"));
            result.add(courseData);
        }
        return result;
    }
}
