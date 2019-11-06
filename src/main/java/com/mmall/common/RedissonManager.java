package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author YaningLiu
 * @date 2018/9/29/ 13:33
 */
@Component
@Slf4j
public class RedissonManager {

    private Config config = new Config();

    private Redisson redisson = null;

    public Redisson getRedisson() {
        return redisson;
    }


    @PostConstruct
    /*
     在具体Bean的实例化过程中，@PostConstruct注释的方法，会在构造方法之后，init方法之前进行调用。

     应用 PostConstruct 注释的方法必须遵守以下所有标准：
     该方法不得有任何参数，除非是在 EJB 拦截器 (interceptor) 的情况下，根据 EJB 规范的定义，在这种情况下它将带有一个 InvocationContext 对象 ；
     该方法的返回类型必须为 void；该方法不得抛出已检查异常；
     应用 PostConstruct 的方法可以是 public、protected、package private 或 private；
     除了应用程序客户端之外，该方法不能是 static；
     该方法可以是 final；
     如果该方法抛出未检查异常，那么不得将类放入服务中，除非是能够处理异常并可从中恢复的 EJB。

     参照@PreDestroy
      */
    private void init() {
        PropertiesUtil.init("redis.properties");

        String redis1Ip = PropertiesUtil.getProperty("redis.sharded.host1").trim();
        String redis1Port = PropertiesUtil.getProperty("redis.sharded.port1").trim();
        String redis2Ip = PropertiesUtil.getProperty("redis.sharded.host2").trim();
        String redis2Port = PropertiesUtil.getProperty("redis.sharded.port2").trim();

        try {
            // 格式：host:port
            config.useSingleServer().setAddress(new StringBuilder().append(redis1Ip).append(":").append(redis1Port).toString());

            redisson = (Redisson) Redisson.create(config);

            log.info("初始化Redisson结束");
        } catch (Exception e) {
            log.error("redisson init error", e);
        }
    }

}
