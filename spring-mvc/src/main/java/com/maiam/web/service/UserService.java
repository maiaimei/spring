package com.maiam.web.service;

import com.maiam.web.annotation.Autowired;
import com.maiam.web.annotation.Service;
import com.maiam.web.mapper.UserMapper;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public String insert()
    {
        System.out.println("业务层 - 新增用户");
        return userMapper.insert();
    }

    public String update()
    {
        System.out.println("业务层 - 更新用户");
        return userMapper.update();
    }

}