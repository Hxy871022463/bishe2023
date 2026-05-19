package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@ApiModel(value = "Course对象", description = "")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("课程名称")
    private String name;

    @ApiModelProperty("学分")
    private Integer score;

    @ApiModelProperty("上课时间")
    private String times;

    @ApiModelProperty("是否开课")
    private Boolean state;

    @ApiModelProperty("授课老师id")
    private Integer teacherId;

    @ApiModelProperty("课程容量")
    private Integer capacity;

    @ApiModelProperty("已选人数")
    private Integer enrolled;

    @TableField(exist = false)
    private String teacher;

    @TableField(exist = false)
    private Integer scId;

}
