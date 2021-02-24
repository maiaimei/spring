package com.maiam.web.controller;

import com.maiam.web.annotation.Autowired;
import com.maiam.web.annotation.Controller;
import com.maiam.web.annotation.RequestMapping;
import com.maiam.web.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("/insert")
    public void insert(HttpServletRequest request, HttpServletResponse response, String name) {
        System.out.println("控制层 - 新增用户");
        String result = userService.insert();
        try {
            request.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().write(String.format("%s，操作成功！<a href=\"/index.jsp\">返回首页</a>",result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/update")
    public void update(HttpServletRequest request, HttpServletResponse response, String name) {
        System.out.println("控制层 - 更新用户");
        String result = userService.update();
        try {
            request.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
