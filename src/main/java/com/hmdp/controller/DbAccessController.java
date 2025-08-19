package com.hmdp.controller;

import java.util.List;

import com.hmdp.dto.Result;
import com.hmdp.utils.DbCounter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DbAccessController {
    @GetMapping("/dbcount") 
    public Result getDbAccessInfo()
    {
        System.out.println("================数据库访问情况=================");
        System.out.println("数据库访问次数："+DbCounter.getCount());
        System.out.println("详情：");
        List<String> threads=DbCounter.getThreads();
        for(String thread:threads) {
            System.out.println(thread);
        }
        System.out.println("=============================================");
        return Result.ok("已经输出信息到控制台");
    }

    @GetMapping("/resetdbcount") 
    public Result resetDbCounter() {
        DbCounter.reset();
        return Result.ok("已重置数据库访问计数器");
    }

}
