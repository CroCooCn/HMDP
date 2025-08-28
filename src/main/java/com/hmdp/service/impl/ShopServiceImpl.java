package com.hmdp.service.impl;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPELIST_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_TTL;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_VALUE_PREF;

import java.lang.management.ThreadInfo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.config.CacheConfig;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.DbCounter;
import com.hmdp.utils.RedisData;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private IShopTypeService typeService;


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheConfig cacheConfig;


    @Override
    public Result queryById(Long id) {
        if("mutex".equals(cacheConfig.getBreakdown())) {
            return queryByIdWithMutex(id);
        }else if("logical-expire".equals(cacheConfig.getBreakdown())) {
            return queryByIdWithLogicalExpire(id);
        }
        return Result.fail("配置文件中app:cache配置错误!");
    }

    Result queryByIdWithMutex(Long id) {
        String lock=LOCK_SHOP_KEY+id;

        //从redis中查询商铺缓存，是一个json信息字符串
        String shopJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);

        //该id的店铺不存在且已在redis中做了缓存
        if("".equals(shopJson)) {
            return Result.fail("redis中记录了该店铺不存在");
        }
        
        //店铺存在且在redis中做了缓存
        if(StrUtil.isNotBlank(shopJson)) {
            Shop cachedShop = JSONUtil.toBean(shopJson,Shop.class);;
            return Result.ok(cachedShop);
        }
        
        //获取不到锁
        if(tryAddLock(lock)==false) {
            //等待锁被释放
            while(!tryAddLock(lock)) {
                try{
                Thread.sleep(50); 
                }catch(Exception e) {}                
            }
            Result res=queryByIdWithMutex(id);
            relLock(lock);
            return res;
        }

        
        //从数据库中查询商铺
        Shop shop=getById(id);
        DbCounter.increment("queryByIdWithMutex");
        //为模拟缓存击穿场景进行延迟（TODO:测试用）
        /*try {
            Thread.sleep(200);
        }catch(Exception e) {}*/

        if(shop==null) {
            //用redis储存key-""来表示空，设置较短的TTL来避免数据库中这个id的商铺被创建后信息不一致
            stringRedisTemplate.opsForValue().set(
                CACHE_SHOP_KEY+id,
                "",
                CACHE_NULL_TTL,
                TimeUnit.MINUTES
            );
            return Result.fail("店铺在数据库中不存在");
        }

        //将商铺信息保存到redis中
        //给商铺信息增加过期时间
        stringRedisTemplate.opsForValue().set(
            CACHE_SHOP_KEY+id,
            JSONUtil.toJsonStr(shop),
            CACHE_SHOP_TTL, TimeUnit.MINUTES
        );
        //释放锁
        relLock(lock);

        return Result.ok(shop);
    }

    //目前的逻辑过期方案需要店铺不被更新
    Result queryByIdWithLogicalExpire(Long id) {
        String shopJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        if(StrUtil.isBlank(shopJson)) {
            return Result.fail("店铺不存在");
        }
        //反序列化为RedisData对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        
        //判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            //未过期，直接返回店铺信息
            return Result.ok(shop);
        }
        
        //已过期，需要缓存重建
        //获取互斥锁
        String lock = LOCK_SHOP_KEY + id;
        if(tryAddLock(lock)) {
            //获取锁成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                saveShopToRedis(id);
                relLock(lock);
                //模拟延迟(TODO:测试用)
                /*try {
                    Thread.sleep(200);
                }
                catch(Exception e) {}*/
            });
        }
        return Result.ok(shop);
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR=Executors.newFixedThreadPool(10);

    //逻辑过期方案才使用
    public void saveShopToRedis(Long id) {
        DbCounter.increment("saveShopToRedis");
        Shop shop = getById(id);
        RedisData  redisData=new RedisData();
        redisData.setData(shop);
        // TODO:测试用，实际使用改成第二行的
        //redisData.setExpireTime(LocalDateTime.now().plusSeconds(10));
        redisData.setExpireTime(LocalDateTime.now().plusMinutes(CACHE_SHOP_TTL));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    public void preloadShopsToRedis() {
        List<Shop> shops=list();
        for(Shop shop:shops) {
            saveShopToRedis(shop.getId());
        }
    }
    
    boolean tryAddLock(String lock) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lock,
        LOCK_VALUE_PREF+Thread.currentThread().getId(),
        LOCK_SHOP_TTL,TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    private boolean relLock(String key) {
        String obtainedValue=stringRedisTemplate.opsForValue().get(key);
        if((LOCK_VALUE_PREF+Thread.currentThread().getId()).equals(obtainedValue))   
            return stringRedisTemplate.delete(key);
        return false;
    }

    @Override
    public Result queryTypeList() {
        String shoptypelistJson=stringRedisTemplate.opsForValue().get(CACHE_SHOPTYPELIST_KEY);


        if(StrUtil.isNotBlank(shoptypelistJson)) {
            List<ShopType> shoptypelist = JSONUtil.toList(shoptypelistJson,ShopType.class);
            return Result.ok(shoptypelist);
        }
        
        DbCounter.increment("queryTypeList");
        List<ShopType> shoptypelist=typeService
        .query().orderByAsc("sort").list();;
        if(shoptypelist==null) {
            return Result.fail("店铺类型列表在数据库中不存在");
        }

        //给shoptype的缓存也增加过期时间
        stringRedisTemplate.opsForValue().set(
            CACHE_SHOPTYPELIST_KEY,
            JSONUtil.toJsonStr(shoptypelist),
            CACHE_SHOPTYPE_TTL,TimeUnit.MINUTES
        );


        return Result.ok(shoptypelist);
    }

    @Override
    @Transactional  // 确保数据库更新和缓存删除操作的原子性，如果任一操作失败则回滚
    public Result updateshop(Shop shop) {
        DbCounter.increment("updateshop");
        //更新数据库中的shop
        updateById(shop);
        //删除缓存
        Long id=shop.getId();
        if(id==null) {
            return Result.fail("店铺id为空！");
        }
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }



}
