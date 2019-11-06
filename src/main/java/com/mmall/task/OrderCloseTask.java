package com.mmall.task;

import com.mmall.common.Const;
import com.mmall.common.RedissonManager;
import com.mmall.service.IOrderService;
import com.mmall.util.PropertiesUtil;
import com.mmall.util.RedisShardedPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * @author YaningLiu
 * @date 2018/9/28/ 11:49
 */
@Component
@Slf4j
public class OrderCloseTask {

    @Autowired
    private IOrderService orderService;
    @Autowired
    private RedissonManager redissonManager;



    @PreDestroy
    // @PreDestroy注解作用就是在spring容器销毁这个类对象前会调用这个方法，参照@PostConstruct
    public void delLock() {
        RedisShardedPoolUtil.del(Const.RedisLock.CLOSE_ORDER_TASK_LOCK);

    }

    public void closeOrderTaskV1() {
        log.info("关闭订单定时任务启动");
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour", "2"));
//        iOrderService.closeOrder(hour);
        log.info("关闭订单定时任务结束");
    }
    /*
    缺陷：在同一时间会有多个tomcat同时进行任务，
    容易造成数据的错乱，
    浪费服务器和mysql的性能
     */


    public void closeOrderTaskV2() {
        log.info("关闭订单定时任务启动");

        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout", "5000"));

        Long setnxResult = RedisShardedPoolUtil.setnx(Const.RedisLock.CLOSE_ORDER_TASK_LOCK, String.valueOf(System.currentTimeMillis() + lockTimeout));
        if (setnxResult != null && setnxResult.intValue() == 1) {
            //如果返回值是1，代表设置成功，获取锁
            closeOrder(Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
        } else {
            log.info("没有获得分布式锁:{}", Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
        }
        log.info("关闭订单定时任务结束");
    }
    /*
    缺陷：
    当其中某台服务器在获取锁后还没来得及设置过期时间，崩了或者重启了。
    那么就会出现死锁的情况。
     */


    public void closeOrderTaskV3() {
        log.info("关闭订单定时任务启动");
        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout", "5000"));
        Long setnxResult = RedisShardedPoolUtil.setnx(Const.RedisLock.CLOSE_ORDER_TASK_LOCK, String.valueOf(System.currentTimeMillis() + lockTimeout));
        if (setnxResult != null && setnxResult.intValue() == 1) {
            closeOrder(Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
        } else {
            //说明有正在服务器在进行任务，获取这个键的值进行继续判断，判断时间戳，看是否可以重置并获取到锁
            String lockValueStr = RedisShardedPoolUtil.get(Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
                /*
                 只有当当前时间戳超过了所存储的时间戳，说明这个存储的值已经是不可用的了。
                 哪怕是未设置过expire，ttl是-1，同样都视为已过期
                  */
            if (lockValueStr != null && System.currentTimeMillis() > Long.parseLong(lockValueStr)) {

                String getSetResult = RedisShardedPoolUtil.getSet(Const.RedisLock.CLOSE_ORDER_TASK_LOCK, String.valueOf(System.currentTimeMillis() + lockTimeout));
                /*
                再次用当前时间戳getset设置值，同时获取旧值，
                返回给定的key的旧值，->旧值判断，是否可以获取锁
                当key没有旧值时，即key不存在时，返回nil（java中对应的就是null） ->获取锁
                 */
                if (getSetResult == null || (getSetResult != null && StringUtils.equals(lockValueStr, getSetResult))) {
                    // 当这个锁获取不到旧值，说明这个锁在redis中已经被删除。
                    // 或是getSetResult和之前获取的lockValueStr若是相等的，说明这个锁没有被其他的服务器所修改

                    //真正获取到锁
                    closeOrder(Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
                } else {
                    log.info("没有获取到分布式锁:{}", Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
                }
            } else {
                log.info("没有获取到分布式锁:{}", Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
            }
        }
        log.info("关闭订单定时任务结束");
    }

    @Scheduled(cron = "0 */1 * * * ?")
    public void myCloseOrderTask() {
        log.info("关闭订单定时任务启动");
        String key = Const.RedisLock.CLOSE_ORDER_TASK_LOCK;
        Long timeOut = Long.parseLong(PropertiesUtil.getProperty("lock.timeout"));
        Long setnxResult = RedisShardedPoolUtil.setnx(key, String.valueOf(System.currentTimeMillis() + timeOut));
        if (null != setnxResult && setnxResult.intValue() == 1) {
            closeOrder(key);
        } else {
            // 说明这个key已经存在设置不了值，则获取这个值
            String lockValue = RedisShardedPoolUtil.get(key);

            if (lockValue != null && System.currentTimeMillis() > Long.parseLong(lockValue)) {
                // 说明锁已经过期失效。重新对这个key进行设置值，并返回久的值
                String getSetValue = RedisShardedPoolUtil.getSet(key, String.valueOf(System.currentTimeMillis() + timeOut));

                // 判断是否有被集群中其他同样获取不到锁的tomcat进行getSet，
                boolean flag = null == getSetValue || getSetValue != null && StringUtils.equals(getSetValue, lockValue);
                if (flag) {
                    closeOrder(key);
                } else {
                    log.info("人品不好,并未获取到分布式锁:{},其他服务器已经执行过任务了", Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
                }
            } else {
                log.info("没有获取到分布式锁:{},该锁是在有效期内。 ", Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
            }
        }
        log.info("关闭订单定时任务结束");
    }
    private void closeOrder(String lockName) {
        //有效期5秒，防止死锁
        RedisShardedPoolUtil.expire(lockName, 5);

        log.info("获取{},ThreadName:{}", Const.RedisLock.CLOSE_ORDER_TASK_LOCK, Thread.currentThread().getName());
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour", "2"));
        orderService.closeOrder(hour);
        RedisShardedPoolUtil.del(Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
        log.info("释放{},ThreadName:{}", Const.RedisLock.CLOSE_ORDER_TASK_LOCK, Thread.currentThread().getName());
        log.info("===============================");
    }



    // 使用redisson框架
//    @Scheduled(cron="0 */1 * * * ?")
    public void closeOrderTaskV4(){

        RLock lock = redissonManager.getRedisson().getLock(Const.RedisLock.CLOSE_ORDER_TASK_LOCK);
        boolean getLock = false;

        try {
            if(getLock = lock.tryLock(0,5, TimeUnit.SECONDS)){
                /*
                这个witeTime最好是设置成0，避免入坑。
                当这个任务的执行时间并不超过所设置的witeTime，那么就会出现在同一次Scheduled执行的时候，多个服务器会拿到分布式锁，使得锁失效的情况
                 */

                log.info("Redisson获取到分布式锁:{},ThreadName:{}",Const.RedisLock.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
                int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour","2"));
//                iOrderService.closeOrder(hour);
            }else{
                log.info("Redisson没有获取到分布式锁:{},ThreadName:{}",Const.RedisLock.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
            }
        } catch (InterruptedException e) {
            log.error("Redisson分布式锁获取异常",e);
        } finally {
            if(getLock){
                //若获取到锁进行释放锁
                lock.unlock();
                log.info("Redisson分布式锁释放锁");
            }
        }
    }



}
