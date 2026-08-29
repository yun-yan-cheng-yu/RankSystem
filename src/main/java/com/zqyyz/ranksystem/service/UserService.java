package com.zqyyz.ranksystem.service;

import com.zqyyz.ranksystem.entity.User;
import com.zqyyz.ranksystem.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 根据ID查询用户
     */
    public User getUserById(Integer id) {
        return userMapper.selectById(id);
    }

    /**
     * 查询所有用户
     */
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }

    /**
     * 新增用户
     */
    public boolean addUser(User user) {
        int rows = userMapper.insert(user);
        return rows > 0;
    }

    /**
     * 更新用户
     */
    public boolean updateUser(User user) {
        int rows = userMapper.update(user);
        return rows > 0;
    }

    /**
     * 删除用户
     */
    public boolean deleteUser(Integer id) {
        int rows = userMapper.deleteById(id);
        return rows > 0;
    }

    /**
     * 根据用户名模糊查询
     */
    public List<User> searchUsers(String userName) {
        return userMapper.selectByUserName(userName);
    }

    /**
     * 批量新增用户
     */
    public boolean batchAddUsers(List<User> userList) {
        int rows = userMapper.batchInsert(userList);
        return rows > 0;
    }
}