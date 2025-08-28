package com.hmdp.service.impl;

import static com.hmdp.utils.RedisConstants.LOCK_VALUE_PREF;
import static com.hmdp.utils.RedisConstants.LOCK_VOUCHER_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_VOUCHER_TTL;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.config.CacheConfig;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisWorker;
import com.hmdp.utils.UserHolder;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    ISeckillVoucherService seckillVoucherService;

    @Resource
    RedisWorker redisWorker;

    @Resource
    CacheConfig cacheConfig;

    @Resource
    StringRedisTemplate stringRedisTemplate;



    @Override 
    public Result seckillVoucher(Long voucherId) {

        if("optim".equals(cacheConfig.getLock())) {
            //乐观锁未实现一人一单，如果要实现给数据库增加唯一索引(userId-voucherId)
            return seckillVoucherWithOptimLock(voucherId);
        }else {
            return seckillVoucherWithPessimLock(voucherId);
        }
    }


    @Transactional
    public Result seckillVoucherWithPessimLock(Long voucherId) {
        //尝试获取锁
        while(getLock(LOCK_VOUCHER_KEY+voucherId)==false) {
            try {
                Thread.sleep(50);}
            catch(Exception e) {}
        }
        
        // 1. 查询优惠券信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) {
            relLock(LOCK_VOUCHER_KEY+voucherId);
            return Result.fail("优惠券不存在");
        }
        //判断秒杀是否在时间范围内
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            relLock(LOCK_VOUCHER_KEY+voucherId);
            return Result.fail("活动未开始");
        }
        if(voucher.getEndTime().isBefore(LocalDateTime.now())) {
            relLock(LOCK_VOUCHER_KEY+voucherId);
            return Result.fail("活动已结束");
        }
        // 4. 判断库存是否充足
        if(voucher.getStock()<=0) {
            relLock(LOCK_VOUCHER_KEY+voucherId);
            return Result.fail("库存不足");
        }
        // 5. 获取当前用户ID
        Long userId = UserHolder.getUser().getId();

        // 从数据库中查询当前用户是否已经下过该优惠券的订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
           relLock(LOCK_VOUCHER_KEY+voucherId);
            return Result.fail("每人限购一单");
        }

        // 7. 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        // 8. 创建订单 ：用户id，订单id，代金券id
        VoucherOrder voucherOrder=new VoucherOrder();
        voucherOrder.setUserId(userId);
        long orderId=redisWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucher.getVoucherId());
        save(voucherOrder);

        //释放锁
        relLock(LOCK_VOUCHER_KEY+voucherId);
        
        // 9. 返回订单ID
        return Result.ok(orderId);
       
    }

    private boolean getLock(String key) {
        // setIfAbsent方法的第三个参数应为Duration类型
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, LOCK_VALUE_PREF+Thread.currentThread().getId(), LOCK_VOUCHER_TTL,TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    private boolean relLock(String key) {
        String obtainedValue=stringRedisTemplate.opsForValue().get(key);
        if((LOCK_VALUE_PREF+Thread.currentThread().getId()).equals(obtainedValue))   
            return stringRedisTemplate.delete(key);
        return false;
    }

    @Transactional
    public Result seckillVoucherWithOptimLock(Long voucherId) {
        
        // 1. 查询优惠券信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) {
            return Result.fail("优惠券不存在");
        }
        //判断秒杀是否在时间范围内
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            
            return Result.fail("活动未开始");
        }
        if(voucher.getEndTime().isBefore(LocalDateTime.now())) {
            
            return Result.fail("活动已结束");
        }
        // 4. 判断库存是否充足
        if(voucher.getStock()<=0) {
           
            return Result.fail("库存不足");
        }
        // 5. 获取当前用户ID
        Long userId = UserHolder.getUser().getId();

        long tryCnt=3;
        boolean success=false;
        for(long i=1;i<=tryCnt;i++) {
            success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .eq("stock", voucher.getStock())
                    .gt("stock", 0)
                    .update();
            if(success) break;
            //未成功，重新获取优惠券
            try {Thread.sleep(20);}
            catch(Exception e) {}
            voucher = seckillVoucherService.getById(voucherId);
            if(voucher.getStock()<=0) {
                return Result.fail("库存不足");
            }
        }
        if(success==false) {
            return Result.fail("秒杀抢购失败");
        }

        // 8. 创建订单 ：用户id，订单id，代金券id
        VoucherOrder voucherOrder=new VoucherOrder();
        voucherOrder.setUserId(userId);
        long orderId=redisWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucher.getVoucherId());
        save(voucherOrder);

        // 9. 返回订单ID
        return Result.ok(orderId);       
    }
    
}
