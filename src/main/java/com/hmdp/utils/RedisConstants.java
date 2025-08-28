package com.hmdp.utils;

import cn.hutool.core.lang.UUID;

public class RedisConstants {
    //在redis中储存发送的验证码（用phone区分）
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 30L;  //分钟
    //在redis中储存用户的登录情况（用token辨别）
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;  //分钟
    //redis中所有值为""的键值对的过期时间
    public static final Long CACHE_NULL_TTL = 2L;   //分钟

    //在redis中储存哪些商铺有缓存（用id区分）
    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    //在redis中储存商铺类型的列表
    public static final Long CACHE_SHOPTYPE_TTL = 30L;
    public static final String CACHE_SHOPTYPELIST_KEY="cache:shoptypelist";

    public static final String LOCK_VALUE_PREF=UUID.randomUUID(true).toString()+"-";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;   //秒

    public static final String LOCK_VOUCHER_KEY = "lock:voucher:";
    public static final Long LOCK_VOUCHER_TTL = 10L;   //秒
    

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
}
