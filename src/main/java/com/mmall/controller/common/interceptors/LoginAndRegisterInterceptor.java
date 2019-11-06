package com.mmall.controller.common.interceptors;

import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.controller.backend.ProductManagerController;
import com.mmall.controller.backend.UserManageController;
import com.mmall.controller.potal.ProductController;
import com.mmall.controller.potal.UserController;
import com.mmall.pojo.User;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;

/**
 * @author YaningLiu
 * @date 2018/10/10/ 11:16
 */
@Slf4j
public class LoginAndRegisterInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        String methodName = handlerMethod.getMethod().getName();
        String className = handlerMethod.getBean().getClass().getSimpleName();

        boolean isManageLoginReq = StringUtils.equals(UserManageController.class.getSimpleName(), className) && StringUtils.equals("login", methodName);
        boolean isUserLoginOrRegisterReq = StringUtils.equals(UserController.class.getSimpleName(), className) && (StringUtils.equals("login", methodName) || StringUtils.equals("register", methodName));

        User user = null;
        /*
        从用户登录状态的最后一次操作时间加上过期时间与当前时间来看用户是否已经过期
        若是有效的直接从当前的session中获取用户对象。
        不存在这个对象，或者是时间已经超过，那么从redis中进行获取。
        redis中取不到说明用户登录是真的已经过期，需要到数据库中找相应的记录。
          */
        Date lastLoginAccessTime = (Date) request.getSession().getAttribute(Const.LAST_LOGIN_ACCESS_TIME);
        if (lastLoginAccessTime != null && lastLoginAccessTime.getTime() + Const.RedisCacheExtime.REDIS_SESSION_EXTIME_MS > System.currentTimeMillis()) {
            // 当最后一次登录后的访问时间加上超时时间大于当前时间，说明登录后的30分钟内是有进行操作的
            user = (User) request.getSession().getAttribute(Const.CURRENT_USER);
            if (user != null) {
                return true;
            }
        }

        String token = CookieUtil.readLoginToken(request);
        if (StringUtils.isNotEmpty(token)) {
            String redisKey = Const.REDIS_USER_SESSION_KEY + token;
            String jsonValue = RedisShardedPoolUtil.get(redisKey);
            user = JsonUtil.string2Obj(jsonValue, User.class);
        }



        // 若是登录请求，则直接return true;
        if (isManageLoginReq || isUserLoginOrRegisterReq) {
            log.info("登录拦截器拦截到{}请求,className:{}", methodName, className);
            request.getSession().setAttribute(Const.CURRENT_USER,user);
            return true;
        }



        boolean notProductReq = !StringUtils.equals(ProductController.class.getSimpleName(), className);
        if (null == user && notProductReq) {
            // 重置response
            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");

            // 获取向页面输出的流
            PrintWriter printWriter = response.getWriter();

            if (StringUtils.equals(className, ProductManagerController.class.getSimpleName()) && StringUtils.equals(methodName, "richtextImgUpload")) {
                Map resultMap = Maps.newHashMap();
                resultMap.put("success", false);
                resultMap.put("msg", "请登录管理员");
                printWriter.print(JsonUtil.obj2String(resultMap));
            } else {
                // 点开首页会先请求这两个接口，进行一些信息的展示，若返回NEED_LOGIN，那么只要进入首页，就会进行跳转到登录页面
                String getCartProductCount = "getCartProductCount";
                String getUserInfoReq = "getUserInfo";
                if (StringUtils.equals(methodName, getUserInfoReq) || StringUtils.equals(methodName, getCartProductCount)) {
                    printWriter.print(JsonUtil.obj2String(ServerResponse.createByErrorMsg("拦截器拦截,用户未登录或登录已过期,无法获取当前用户的信息")));
                } else {
                    printWriter.print(JsonUtil.obj2String(ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "拦截器拦截,用户未登录或登录已过期,无法获取当前用户的信息")));
                }
            }

            printWriter.flush();
            printWriter.close();

            // 若并未进行登录，或者是redis中的信息已经超时，被删除，将存储在session中的用户对象删除。
            request.getSession().removeAttribute(Const.CURRENT_USER);
            return false;
        }
        // 供controller和权限拦截器获取进行权限校验，省略redis中取user信息
        request.getSession().setAttribute(Const.CURRENT_USER, user);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
