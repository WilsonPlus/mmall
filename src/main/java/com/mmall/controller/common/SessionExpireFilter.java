package com.mmall.controller.common;

import com.mmall.common.Const;
import com.mmall.pojo.User;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;

/**
 * @author YaningLiu
 * @date 2018/9/25/ 21:06
 */
public class SessionExpireFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        String token = CookieUtil.readLoginToken(httpServletRequest);
        if (StringUtils.isNotEmpty(token)) {
            String key = Const.REDIS_USER_SESSION_KEY + token;

            String userJsonValue = RedisShardedPoolUtil.get(key);
            User user = JsonUtil.string2Obj(userJsonValue, User.class);
            if (user != null) {
                RedisShardedPoolUtil.expire(key, Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
                ((HttpServletRequest) request).getSession().setAttribute(Const.LAST_LOGIN_ACCESS_TIME, new Date());
            }
        }
        chain.doFilter(request,response);
    }

    @Override
    public void destroy() {

    }
}
