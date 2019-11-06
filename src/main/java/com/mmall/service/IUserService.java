package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;

/**
 * @author YaningLiu
 * @date 2018/9/6/ 19:01
 */
public interface IUserService {
    /**
     * 根据用户名和密码进行登录
     *
     * @param username 用户名
     * @param password 密码
     * @return
     */
    public ServerResponse<User> login(String username, String password);

    /**
     * 用户注册
     *
     * @param user user对象
     * @return
     */
    public ServerResponse<String> register(User user);

    /**
     * 检查某个字段的值是否可用
     *
     * @param value 值
     * @param type  字段
     * @return
     */
    public ServerResponse<String> checkValid(String value, String type);

    /**
     * 根据用户名来查询对应的忘记密码问题
     *
     * @param username  用户名
     * @return
     */
    public ServerResponse<String> selectQuestion(String username);

    /**
     * 检查该用户的问题答案是否正确
     *
     * @param username 用户名
     * @param question 问题
     * @param answer   问题答案
     * @return
     */
    public ServerResponse<String> checkAnswer(String username, String question, String answer);

    /**
     * 忘记密码，进行重置密码
     *
     * @param username    用户名
     * @param token       token
     * @param passwordNew 新密码
     * @return
     */
    public ServerResponse<String> forgetResetPassword(String username, String token, String passwordNew);

    /**
     * 已经登录后进行重置密码
     *
     * @param passwordOld   旧密码
     * @param passwordNew   新密码
     * @param userId  user的id
     * @return
     */
    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, Integer userId);

    /**
     * 更新用户信息
     *
     * @param user  user对象
     * @return
     */
    public ServerResponse<User> updateInformation(User user);

    /**
     * 根据用户id获取用户信息
     *
     * @param userId    用户id
     * @return
     */
    public ServerResponse<User> getInfoById(Integer userId);

    /**
     * 判断传递过来的用户是否是管理员
     * @param user  user对象
     * @return
     */
    boolean isAdminRole(User user);
}
