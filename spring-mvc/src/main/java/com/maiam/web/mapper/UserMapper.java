package com.maiam.web.mapper;

import com.maiam.web.annotation.Repository;

@Repository
public class UserMapper {

    public String insert(){
        System.out.println("持久层 - 新增用户");
        return "新增用户";
    }

    public String update(){
        System.out.println("持久层 - 更新用户");
        return "更新用户";
    }

}