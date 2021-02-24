package com.maiam.spring.service;

import com.maiam.spring.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

//@Scope(value="prototype")
@Component
public class UserService {

    @Autowired
    private UserDao userDao;

    public void create(){
        userDao.create();
    }

    @PostConstruct
    public void init() {
        System.out.println("UserService init...");
    }

    // 这个没执行
    @PreDestroy
    public void destroy() {
        System.out.println("UserService destroy...");
    }

}