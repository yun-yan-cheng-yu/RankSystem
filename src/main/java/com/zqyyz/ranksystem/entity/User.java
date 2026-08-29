package com.zqyyz.ranksystem.entity;

public class User {
    private Integer id;
    private String userName;
    private String password;
    private Integer age;
    private String email;

    // 无参构造方法
    public User() {
    }

    // 有参构造方法（可选）
    public User(Integer id, String userName, String password, Integer age, String email) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.age = age;
        this.email = email;
    }

    // getter 和 setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // toString 方法（方便调试）
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                '}';
    }
}