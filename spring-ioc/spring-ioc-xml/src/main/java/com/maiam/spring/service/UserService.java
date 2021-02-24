package com.maiam.spring.service;

import com.maiam.spring.dao.UserDao;

public class UserService {

    private UserDao userDao;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public void create(){
        userDao.create();
    }

}
