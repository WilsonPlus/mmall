package com.mmall.common;

/**
 * @author YaningLiu
 * @date 2018/9/6/ 19:36
 */
public enum ResponseCode {
    SUCCESS(0, "SUCCESS"),
    ERROR(1, "ERROR"),
    ILLEGAL_ARGUMENT(2, "ILLEGAL_ARGUMENT"),
    NEED_LOGIN(10, "NEED_LOGIN");


    ResponseCode(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private final String desc;
    private final int code;

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
