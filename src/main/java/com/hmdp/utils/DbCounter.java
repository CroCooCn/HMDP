package com.hmdp.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cn.hutool.core.util.StrUtil;

public class DbCounter {
    //原子整数，用于并发场景
    private static final AtomicInteger count=new AtomicInteger(0);

    private static final List<String> threads = Collections.synchronizedList(new ArrayList<>());

    public static void increment(String tread) {
        count.incrementAndGet();
        if(StrUtil.isBlank(tread)) tread="未写明";
        threads.add(tread);
    }

    public static int getCount() {
        return count.get();
    }

    public static List<String> getThreads() {
        return new ArrayList<>(threads);
    }

    public static void reset() {
        threads.clear();
        count.set(0);
    }

}
