package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 课程评价实体类
 */
@Data
@TableName("sys_comment")
@ApiModel(value = "Comment对象", description = "课程评价/评论表")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("评价内容")
    private String content;

    @ApiModelProperty("评价人ID")
    private Integer userId;

    @ApiModelProperty("课程ID")
    private Integer courseId;

    @ApiModelProperty("评分 (1-5星)")
    private Integer rate;

    @ApiModelProperty("评价时间")
    private String time;

    @ApiModelProperty("父级评论ID")
    private Integer parentId;

    @ApiModelProperty("根评论ID")
    private Integer originId;

    @TableField(exist = false)
    @ApiModelProperty("评价人昵称")
    private String nickname;

    @TableField(exist = false)
    @ApiModelProperty("评价人头像")
    private String avatarUrl;

    @TableField(exist = false)
    @ApiModelProperty("子评论列表")
    private List<Comment> children;
}
