package com.mmall.common;

import com.google.common.collect.Sets;
import lombok.Getter;

import java.util.Set;

/**
 * 一个公共的常量类
 *
 * @author YaningLiu
 * @date 2018/9/6/ 21:40
 */
public class Const {
    public static final String TOKEN_PREFIX = "froget_token:";
    public static final Integer FORGET_TOKEN_EXTIME = 12 * 60 * 60;
    public static final String REDIS_USER_SESSION_KEY = "redis_user_session:";
    public static final String CURRENT_USER = "currentUser";
    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    // 最后登录的访问时间
    public static final String LAST_LOGIN_ACCESS_TIME = "lastLoginAccessTime";
    public static final Set PRODUCT_SORT_COLUMN = Sets.newHashSet("price_desc", "price_asc", "name_asc", "name_desc");

    @Getter
    public enum ProductStatusEnum {
        ON_SALE(1, "在线");

        private String value;
        private int code;

        ProductStatusEnum(int code, String value) {
            this.code = code;
            this.value = value;
        }

    }

    public enum OrderStatusEnum {
        CANCELED(0, "已取消"),
        NO_PAY(10, "未支付"),
        PAID(20, "已付款"),
        SHIPPED(40, "已发货"),
        ORDER_SUCCESS(50, "订单完成"),
        ORDER_CLOSE(60, "订单关闭");


        OrderStatusEnum(int code, String value) {
            this.code = code;
            this.value = value;
        }

        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }

        public static OrderStatusEnum codeOf(int code) {
            for (OrderStatusEnum orderStatusEnum : values()) {
                if (orderStatusEnum.getCode() == code) {
                    return orderStatusEnum;
                }
            }
            throw new RuntimeException("么有找到对应的枚举");
        }
    }

    public enum PayPlatformEnum {
        ALIPAY(1, "支付宝");

        PayPlatformEnum(int code, String value) {
            this.code = code;
            this.value = value;
        }

        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }
    }

    public enum PaymentTypeEnum {
        ONLINE_PAY(1, "在线支付");

        PaymentTypeEnum(int code, String value) {
            this.code = code;
            this.value = value;
        }

        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }

        public static PaymentTypeEnum codeOf(Integer code) {
            for (PaymentTypeEnum paymentTypeEnum : values()) {
                if (paymentTypeEnum.getCode() == code) {
                    return paymentTypeEnum;
                }
            }
            throw new RuntimeException("么有找到对应的枚举");
        }
    }

    public interface RedisCacheExtime {
        int REDIS_SESSION_EXTIME = 30 * 60;
        long REDIS_SESSION_EXTIME_MS = 30 * 60 * 1000;
    }

    public interface RedisLock {
        String CLOSE_ORDER_TASK_LOCK = "CLOSE_ORDER_TASK_LOCK";
    }

    public interface Role {
        // 普通用户
        int ROLE_ADMIN = 1;
        // 管理员身份
        int ROLE_CUSTOMER = 0;
    }

    public interface AlipayCallback {
        String TRADE_STATUS_WAIT_BUYER_PAY = "WAIT_BUYER_PAY";
        String TRADE_STATUS_TRADE_SUCCESS = "TRADE_SUCCESS";

        String RESPONSE_SUCCESS = "success";
        String RESPONSE_FAILED = "failed";
    }

    public interface Cart {
        //即购物车中商品选中状态
        int CHECKED = 1;
        //购物车中商品未选中状态
        int UN_CHECKED = 0;

        String LIMIT_NUM_FAIL = "LIMIT_NUM_FAIL";
        String LIMIT_NUM_SUCCESS = "LIMIT_NUM_SUCCESS";
    }
}
