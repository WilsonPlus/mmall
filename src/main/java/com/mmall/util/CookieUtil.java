package com.mmall.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author YaningLiu
 * @date 2018/9/25/ 10:28
 */
@Slf4j
public class CookieUtil {
    private final static String COOKIE_DOMAIN;
    private final static String COOKIE_NAME;

    static {
        COOKIE_DOMAIN = PropertiesUtil.getProperty("cookie.domain");
        COOKIE_NAME = PropertiesUtil.getProperty("cookie.name");
    }

    public static String readLoginToken(HttpServletRequest request) {
        Cookie[] cks = request.getCookies();
        if (cks != null) {
            for (Cookie ck : cks) {
                log.info("read cookieName:{},cookieValue:{}", ck.getName(), ck.getValue());
                if (StringUtils.equals(ck.getName(), COOKIE_NAME)) {
                    log.info("return cookieName:{},cookieValue:{}", ck.getName(), ck.getValue());
                    return ck.getValue();
                }
            }
        }
        return null;
    }

    public static void writeLoginToken(HttpServletResponse response, String sessionId) {
        Cookie ck = new Cookie(COOKIE_NAME, sessionId);
        ck.setDomain(COOKIE_DOMAIN);
        //代表设置在根目录
        ck.setPath("/");
        /*
        不允许通过脚本来获取cookie，降低脚本攻击带来的信息泄漏风险
        tomcat7默认是servlet3.0，所以这个属性可用之间设置。
         */
        ck.setHttpOnly(true);
        // 设置cookie的有效期，单位：秒；不设置的话，cookie就不会写入硬盘，而是写在内存。只在当前页面有效。
        //-1，代表永久
        ck.setMaxAge(60 * 60 * 24 * 365);
        log.info("write cookieName:{},cookieValue:{}", ck.getName(), ck.getValue());
        response.addCookie(ck);
    }


    public static void delLoginToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cks = request.getCookies();
        if (cks != null) {
            for (Cookie ck : cks) {
                if (StringUtils.equals(ck.getName(), COOKIE_NAME)) {
                    ck.setDomain(COOKIE_DOMAIN);
                    ck.setPath("/");
                    //设置成0，代表删除此cookie。
                    ck.setMaxAge(0);
                    log.info("del cookieName:{},cookieValue:{}", ck.getName(), ck.getValue());
                    response.addCookie(ck);
                    return;
                }
            }
        }
    }


}

