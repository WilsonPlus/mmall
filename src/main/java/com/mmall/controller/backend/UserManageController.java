package com.mmall.controller.backend;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author YaningLiu
 * @date 2018/9/7/ 18:29
 */
@Controller
@RequestMapping("/manage/user")
public class UserManageController {
    private final IUserService userService;

    @Autowired
    public UserManageController(IUserService userService) {
        this.userService = userService;
    }

    @RequestMapping(value = "/login.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(HttpServletRequest request,
                                      String username, String password,
                                      HttpServletResponse response) {
        HttpSession session = request.getSession();

        /*
         先从session中取用户对象，前面的拦截器已经从redis中获取过用户数据转换成对象了，
         若对象为null，说明redis中不存在该数据，需要进行登录。
          */
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user != null){
            return ServerResponse.createBySuccessMsg("您已登录,请不要重复登录!");
        }

        // 从redis中取不到用户数据，再从数据库中找该用户记录
        ServerResponse<User> res = userService.login(username, password);
        if (res.isSuccess()) {
            user = res.getData();
            session.setAttribute(Const.CURRENT_USER,user);
            try {
                if (userService.isAdminRole(user)) {
                    return ServerResponse.createBySuccess(user);
                }
                return ServerResponse.createBySuccessMsg("您并非管理人员");
            } finally {
                CookieUtil.writeLoginToken(response, session.getId());
                String key = Const.REDIS_USER_SESSION_KEY + session.getId();
                RedisShardedPoolUtil.setEx(key, JsonUtil.obj2String(res.getData()), Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
            }
        }
        return res;
    }
}
