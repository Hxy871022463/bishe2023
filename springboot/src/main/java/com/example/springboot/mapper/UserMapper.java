package com.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.controller.dto.UserPasswordDTO;
import com.example.springboot.entity.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

//@Mapper
@Repository
public interface UserMapper extends BaseMapper<User> {

//    @Select("SELECT * FROM sys_user")
//    List<User> findAll();
//
//    @Insert("INSERT into sys_user(username,password,nickname,email,phone,address) VALUES (#{username}, #{password}," +
//            " #{nickname}, #{email}, #{phone}, #{address})")
//    int insert(User user);
//
//    int update(User user);
//
//    @Delete("delete from sys_user where id = #{id}")
//    Integer deleteById(@Param("id") Integer id);
//
//    @Select("select * from sys_user where username like #{username} limit #{pageNum}, #{pageSize}")
//    List<User> selectPage(Integer pageNum, Integer pageSize, String username);
//
//    @Select("select count(*) from sys_user where username like concat('%', #{username}, '%') ")
//    Integer selectTotal(String username);
@Update("update sys_user set password = #{newPassword} where username = #{username} and password = #{password}")
int updatePassword(UserPasswordDTO userPasswordDTO);

    Page<User> findPage(Page<User> page, @Param("username") String username, @Param("email") String email, @Param("address") String address);

    List<com.example.springboot.entity.Course> findStudentCourses(@Param("userId") Integer userId);
    List<com.example.springboot.entity.Course> findTeacherCourses(@Param("userId") Integer userId);
}
