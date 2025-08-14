package com.hmdp.service.impl;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPELIST_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_TTL;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.service.IShopTypeService;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.core.util.StrUtil;
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

    @Override
    public Result queryById(Long id) {
        //从redis中查询商铺缓存，是一个json信息字符串
        String shopJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);

        //该id的店铺不存在且已在redis中做了缓存
        if("".equals(shopJson)) {
            return Result.fail("redis中记录了该店铺不存在");
        }
        if(StrUtil.isNotBlank(shopJson)) {
            Shop cachedShop = JSONUtil.toBean(shopJson,Shop.class);;
            return Result.ok(cachedShop);
        }

        //从数据 库中查询商铺
        Shop shop=getById(id);
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


        return Result.ok(shop);
    }

    @Override
    public Result queryTypeList() {
        String shoptypelistJson=stringRedisTemplate.opsForValue().get(CACHE_SHOPTYPELIST_KEY);


        if(StrUtil.isNotBlank(shoptypelistJson)) {
            List<ShopType> shoptypelist = JSONUtil.toList(shoptypelistJson,ShopType.class);
            return Result.ok(shoptypelist);
        }
        
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
