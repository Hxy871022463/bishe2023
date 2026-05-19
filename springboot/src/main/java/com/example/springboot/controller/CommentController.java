package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Comment;
import com.example.springboot.service.CommentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 课程评价接口层
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private CommentService commentService;

    /**
     * 发表评价
     */
    @PostMapping
    public Result save(@RequestBody Comment comment) {
        commentService.saveComment(comment);
        return Result.success();
    }

    /**
     * 查询课程评价树
     */
    @GetMapping("/tree/{courseId}")
    public Result findTree(@PathVariable Integer courseId) {
        return Result.success(commentService.findTree(courseId));
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        commentService.removeById(id);
        return Result.success();
    }
}
