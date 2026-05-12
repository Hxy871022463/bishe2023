package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @GetMapping("/hotCourses")
    public Result getHotCourses() {
        return Result.success(statisticsService.getHotCourses());
    }

    @GetMapping("/rateDistribution")
    public Result getRateDistribution(Integer courseId) {
        return Result.success(statisticsService.getRateDistribution(courseId));
    }

    @GetMapping("/saturation")
    public Result getCourseSaturation() {
        return Result.success(statisticsService.getCourseSaturation());
    }
}
