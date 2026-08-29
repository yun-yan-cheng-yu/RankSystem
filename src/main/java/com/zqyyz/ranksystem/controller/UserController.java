package com.zqyyz.ranksystem.controller;

import com.zqyyz.ranksystem.entity.User;
import com.zqyyz.ranksystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
        import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 根据ID查询用户
     * GET /api/users/1
     */
    @GetMapping("/{id}")
    public User getUser(@PathVariable Integer id) {
        return userService.getUserById(id);
    }

    /**
     * 查询所有用户
     * GET /api/users
     */
    @GetMapping
    public List<User> listUsers() {
        return userService.getAllUsers();
    }

    /**
     * 新增用户
     * POST /api/users
     * Body: {"userName":"张三","password":"123456","age":25,"email":"zhangsan@test.com"}
     */
    @PostMapping
    public String addUser(@RequestBody User user) {
        boolean result = userService.addUser(user);
        return result ? "新增成功，ID为：" + user.getId() : "新增失败";
    }

    /**
     * 更新用户
     * PUT /api/users
     * Body: {"id":1,"userName":"张三丰","password":"654321","age":26,"email":"zhangsanfeng@test.com"}
     */
    @PutMapping
    public String updateUser(@RequestBody User user) {
        boolean result = userService.updateUser(user);
        return result ? "更新成功" : "更新失败";
    }

    /**
     * 删除用户
     * DELETE /api/users/1
     */
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Integer id) {
        boolean result = userService.deleteUser(id);
        return result ? "删除成功" : "删除失败";
    }

    /**
     * 根据用户名模糊查询
     * GET /api/users/search?name=张
     */
    @GetMapping("/search")
    public List<User> searchUsers(@RequestParam String name) {
        return userService.searchUsers(name);
    }

    /**
     * 批量新增用户
     * POST /api/users/batch
     * Body: [{"userName":"王五","password":"111","age":30,"email":"wangwu@test.com"}, ...]
     */
    @PostMapping("/batch")
    public String batchAddUsers(@RequestBody List<User> userList) {
        boolean result = userService.batchAddUsers(userList);
        return result ? "批量新增成功，共" + userList.size() + "条" : "批量新增失败";
    }
}