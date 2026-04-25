package com.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springboot.entity.Comment;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentMapper extends BaseMapper<Comment> {
}
