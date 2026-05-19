package com.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.Constants;
import com.example.springboot.entity.Comment;
import com.example.springboot.entity.User;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.CommentMapper;
import com.example.springboot.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程评价 Service 业务层
 */
@Service
public class CommentService extends ServiceImpl<CommentMapper, Comment> {

    @Resource
    private UserService userService;

    /**
     * 保存评价（带安全校验与防重复提交）
     */
    public void saveComment(Comment comment) {
        // 1. 强制获取当前登录用户，防止伪造身份
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException(Constants.CODE_401, "请先登录");
        }
        comment.setUserId(currentUser.getId());

        // 2. 校验：如果是主评价（非回复），防止对同一门课重复评价
        if (comment.getParentId() == null) {
            QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", comment.getUserId())
                    .eq("course_id", comment.getCourseId())
                    .isNull("parent_id");
            if (this.count(queryWrapper) > 0) {
                throw new ServiceException(Constants.CODE_600, "您已评价过该课程，请勿重复提交");
            }
        }

        // 3. 执行保存
        save(comment);
    }

    /**
     * 获取评价树（包含用户信息及回复列表）
     */
    public List<Comment> findTree(Integer courseId) {
        // 1. 查询该课程下所有评价
        List<Comment> allComments = list(new QueryWrapper<Comment>().eq("course_id", courseId));

        // 2. 补全用户信息
        for (Comment comment : allComments) {
            User user = userService.getById(comment.getUserId());
            if (user != null) {
                comment.setNickname(user.getNickname());
                comment.setAvatarUrl(user.getAvatarUrl());
            }
        }

        // 3. 组装父子关系
        List<Comment> rootComments = allComments.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());

        for (Comment root : rootComments) {
            List<Comment> children = allComments.stream()
                    .filter(c -> root.getId().equals(c.getParentId()))
                    .collect(Collectors.toList());
            root.setChildren(children);
        }

        return rootComments;
    }
}