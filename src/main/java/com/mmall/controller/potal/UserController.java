package com.mmall.controller.potal;

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
 * @date 2018/9/6/ 17:31
 */
@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IUserService userService;

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @param session  session
     * @return 返回的是一个高复用的服务响应对象
     */
    @RequestMapping(value = "/login.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(String username, String password,HttpServletRequest request, HttpSession session, HttpServletResponse response) {
        User user = (User) request.getAttribute(Const.CURRENT_USER);
        if(user!=null){
            return ServerResponse.createBySuccess("您已登录，请不要重复！", user);
        }
        ServerResponse<User> res = userService.login(username, password);
        if (res.isSuccess()) {
            CookieUtil.writeLoginToken(response, session.getId());
            String key = Const.REDIS_USER_SESSION_KEY + session.getId();
            RedisShardedPoolUtil.setEx(key, JsonUtil.obj2String(res.getData()), Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
        }
        return res;
    }

    @RequestMapping(value = "/logout.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {

        // 读取到登录用户的sessionId
        String token = CookieUtil.readLoginToken(request);
        // 从cookie中进行删除
        CookieUtil.delLoginToken(request, response);
        // redis中删除
        String key = Const.REDIS_USER_SESSION_KEY + token;
        RedisShardedPoolUtil.del(key);

        request.removeAttribute(Const.CURRENT_USER);
        return ServerResponse.createBySuccess();
    }

    @RequestMapping(value = "/register.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(User user) {
        return userService.register(user);
    }

    @RequestMapping(value = "/checkvalue.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkValue(String value, String type) {
        return userService.checkValid(value, type);
    }

    @RequestMapping(value = "/get_user_info.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpServletRequest request) {
        User user = (User) request.getAttribute(Const.CURRENT_USER);
        return ServerResponse.createBySuccess(user);
    }

    @RequestMapping(value = "/get_information.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getInformation(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute(Const.CURRENT_USER);
        return userService.getInfoById(currentUser.getId());
    }

    /**
     * 忘记密码，获取密码问题
     *
     * @param username 用户名
     * @return 将问题包装返回
     */
    @RequestMapping(value = "/forget_get_question.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username) {
        return userService.selectQuestion(username);
    }

    /**
     * 检查答案是否正确。
     *
     * @param username 用户名
     * @param question 问题
     * @param answer   问题答案
     * @return 校验成功生成的token包装返回
     */
    @RequestMapping(value = "/forget_check_answer.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkAnswer(String username, String question, String answer) {
        return userService.checkAnswer(username, question, answer);
    }

    @RequestMapping(value = "/forget_reset_password.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgeResetPassword(String username, String token, String newPassword) {
        return userService.forgetResetPassword(username, token, newPassword);
    }

    @RequestMapping(value = "/reset_password.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> resetPassword(HttpServletRequest request, String passwordOld, String passwordNew) {
        User user = (User) request.getAttribute(Const.CURRENT_USER);
        return userService.resetPassword(passwordOld, passwordNew, user.getId());
    }

    @RequestMapping(value = "/update_userinfo.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> updateUserInfo(HttpServletRequest request, User user) {

        User currentUser = (User) request.getAttribute(Const.CURRENT_USER);
        String token = CookieUtil.readLoginToken(request);
        String key = Const.REDIS_USER_SESSION_KEY + token;
        // 需要更改的对象的是没有userId的，
        user.setId(currentUser.getId());
        ServerResponse<User> res = userService.updateInformation(user);
        if(res.isSuccess()){
            RedisShardedPoolUtil.setEx(key, JsonUtil.obj2String(res.getData()), Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
        }
        return res;
    }
}
