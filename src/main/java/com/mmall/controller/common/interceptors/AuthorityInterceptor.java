package com.mmall.controller.common.interceptors;

import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.controller.backend.UserManageController;
import com.mmall.pojo.User;
import com.mmall.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

/**
 * @author YaningLiu
 * @date 2018/9/27/ 18:03
 */
@Slf4j
public class AuthorityInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        String methodName = handlerMethod.getMethod().getName();
        String className = handlerMethod.getBean().getClass().getSimpleName();

        StringBuilder requestParamBuilder = new StringBuilder();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Iterator<Map.Entry<String, String[]>> iterator = requestParams.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, String[]> next = iterator.next();
            String key = next.getKey();
            String[] values = next.getValue();
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append((i == values.length - 1) ? values[i] : values[i] + ",");
            }
            //乱码解决，这段代码在出现乱码时使用
            //valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");

            requestParamBuilder.append(key).append("=").append(valueStr);
            if (iterator.hasNext()) {
                requestParamBuilder.append("&");
            }
        }

        boolean isManageLoginReq = StringUtils.equals(UserManageController.class.getSimpleName(), className) && StringUtils.equals("login", methodName);
        if (isManageLoginReq) {
            log.info("权限拦截器拦截到{}请求,className:{}", methodName,className);
            return true;
        }

        log.info("权限拦截器拦截到请求,className:{},methodName:{},request:{}", className, methodName,requestParamBuilder.toString());

        User user = (User) request.getSession().getAttribute(Const.CURRENT_USER);
        // 经过登录拦截器后，登录拦截器将User存储到了session域中

        if (null == user || (user.getRole() != Const.Role.ROLE_ADMIN)) {
            // 重置response
            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");

            // 获取向页面输出的流
            PrintWriter printWriter = response.getWriter();

            //上传由于富文本的控件要求，要特殊处理返回值，这里面区分是否登录以及是否有权限

                if (StringUtils.equals(className, "ProductManageController") && StringUtils.equals(methodName, "richtextImgUpload")) {
                    Map resultMap = Maps.newHashMap();
                    resultMap.put("success", false);
                    resultMap.put("msg", "无权限操作");
                    printWriter.print(JsonUtil.obj2String(resultMap));
                } else {
                    printWriter.print(JsonUtil.obj2String(ServerResponse.createByErrorMsg("拦截器拦截,用户无权限操作")));
                }

            printWriter.flush();
            printWriter.close();

            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
