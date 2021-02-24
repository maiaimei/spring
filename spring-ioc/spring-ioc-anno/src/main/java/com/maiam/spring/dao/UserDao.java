package com.maiam.spring.dao;

import org.springframework.stereotype.Component;

@Component
public class UserDao {
    public void create(){
        System.out.println("创建用户");
    }
}
