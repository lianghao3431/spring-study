package com.lh.spring.mvc.controller;

import com.lh.spring.mvc.annotation.LhController;
import com.lh.spring.mvc.annotation.LhRequestMapping;
import com.lh.spring.mvc.annotation.LhRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @ClassName LhTestController
 * @Description
 * @Date 2019/7/20 17:45
 * @Aurhor liang.hao
 */
@LhController
@LhRequestMapping("/lh")
public class LhTestController {


    @LhRequestMapping("/query.*")
    public void query(HttpServletRequest request , HttpServletResponse response ,
                      @LhRequestParam("name") String name){
        try {
            response.getWriter().write("my name is"+name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @LhRequestMapping("/add")
    public void add(HttpServletRequest request , HttpServletResponse response ,
                      @LhRequestParam("a") Integer a , @LhRequestParam("b") Integer b){
        try {
            int count = a+b;
            response.getWriter().write("a+b="+count);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @LhRequestMapping("/return")
    public String returnName(@LhRequestParam("a") Integer a){
        return a+"";
    }
}
