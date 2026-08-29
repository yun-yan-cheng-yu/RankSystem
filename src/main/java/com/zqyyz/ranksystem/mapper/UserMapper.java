package com.zqyyz.ranksystem.mapper;



import com.zqyyz.ranksystem.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {

    // 根据ID查询用户
    User selectById(@Param("id") Integer id);

    // 查询所有用户
    List<User> selectAll();

    // 新增用户（返回影响行数）
    int insert(User user);

    // 更新用户（返回影响行数）
    int update(User user);

    // 根据ID删除用户（返回影响行数）
    int deleteById(@Param("id") Integer id);

    // 根据用户名模糊查询
    List<User> selectByUserName(@Param("userName") String userName);

    // 批量插入（可选，用于演示复杂操作）
    int batchInsert(@Param("list") List<User> userList);
}