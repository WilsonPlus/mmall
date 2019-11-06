package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import com.mmall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author YaningLiu
 * @date 2018/9/6/ 19:03
 */
@Service
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        // if (userMapper.checkUsername(username) == 0) {
        if (checkValid(username, Const.USERNAME).isSuccess()) {
            return ServerResponse.createByErrorMsg("用户名不存在！");
        }
        User user = userMapper.loginSelect(username, MD5Util.MD5EncodeUtf8(password));
        if (user == null) {
            return ServerResponse.createByErrorMsg("密码错误！");
        }
        user.setPassword(StringUtils.EMPTY);
        user.setAnswer(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功！", user);
    }

    @Override
    public ServerResponse<String> register(User user) {
        // if (userMapper.checkUsername(user.getUsername())>0) {
        ServerResponse<String> res = checkValid(user.getUsername(), Const.USERNAME);
        if (!res.isSuccess()) {
            // 若这个值是不可用的，说明已经存在，返回错误的消息
            return ServerResponse.createByErrorMsg(res.getMsg());
        }
        // if (userMapper.checkEmail(user.getEmail()) > 0) {
        res = checkValid(user.getEmail(), Const.EMAIL);
        if (!res.isSuccess()) {
            return ServerResponse.createByErrorMsg(res.getMsg());
        }
        // 设置用户的角色
        user.setRole(Const.Role.ROLE_CUSTOMER);
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        if (userMapper.insert(user) != 1) {
            return ServerResponse.createByErrorMsg("注册失败！");
        }
        return ServerResponse.createBySuccess("注册成功！");
    }

    @Override
    public ServerResponse<String> checkValid(String value, String type) {
        if (StringUtils.isBlank(value)) {
            return ServerResponse.createByErrorMsg(ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        if (Const.USERNAME.equals(type)) {
            if (userMapper.checkUsername(value) > 0) {
                return ServerResponse.createByErrorMsg("用户名已存在");
            }
        }
        if (Const.EMAIL.equals(type)) {
            if (userMapper.checkEmail(value) > 0) {
                return ServerResponse.createByErrorMsg("email已存在。");
            }
        }
        return ServerResponse.createBySuccessMsg("数据可用");
    }

    @Override
    public ServerResponse<String> selectQuestion(String username) {
        if (this.checkValid(username, Const.USERNAME).isSuccess()) {
            // 先检查数据库中是否存在该条记录，checkValid(),如果可用，说明不存在这条记录。
            return ServerResponse.createByErrorMsg("用户名不存在！");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if (StringUtils.isNoneBlank(question)) {
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMsg("忘记密码问题不存在！");
    }

    @Override
    public ServerResponse<String> checkAnswer(String username, String question, String answer) {
        if (userMapper.checkAnswer(username, question, answer) == 1) {
            // 说明这个问题和答案是匹配的，并且属于这个用户。
            String forgetToken = UUID.randomUUID().toString();
            RedisShardedPoolUtil.setEx(Const.TOKEN_PREFIX + username, forgetToken, Const.FORGET_TOKEN_EXTIME);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByErrorMsg("问题答案错误！");
    }

    @Override
    public ServerResponse<String> forgetResetPassword(String username, String token, String passwordNew) {
        if (StringUtils.isBlank(token)) {
            return ServerResponse.createByErrorMsg("参数错误，token需要进行传递！");
        }
        if (this.checkValid(username, Const.USERNAME).isSuccess()) {
            return ServerResponse.createBySuccessMsg("用户名不存在！");
        }
        String cacheToken = RedisShardedPoolUtil.get(Const.TOKEN_PREFIX + username);

        if (StringUtils.isBlank(cacheToken) || !token.equals(cacheToken)) {
            return ServerResponse.createBySuccessMsg("token无效或者过期");
        }

        passwordNew = MD5Util.MD5EncodeUtf8(passwordNew);
        int rowCount = userMapper.updatePasswordByUsername(username, passwordNew);
        if (rowCount > 0) {
            // 修改密码成功后将token拿掉
            RedisShardedPoolUtil.del(Const.TOKEN_PREFIX + username);
            // 修改后应进行重新登录的

            return ServerResponse.createBySuccessMsg("修改密码成功");
        }
        return ServerResponse.createByErrorMsg("修改密码失败");
    }

    @Override
    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, Integer userId) {
        //防止横向越权,要校验一下这个用户的旧密码,一定要指定是这个用户.因为我们会查询一个count(1),如果不指定id,那么结果就是true啦count>0;
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld), userId);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMsg("旧密码错误");
        }

        User updateTarget = new User();
        updateTarget.setId(userId);

        updateTarget.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount = userMapper.updateByPrimaryKeySelective(updateTarget);
        if (updateCount > 0) {
            return ServerResponse.createBySuccessMsg("密码更新成功");
        }
        return ServerResponse.createByErrorMsg("密码更新失败");
    }

    @Override
    public ServerResponse<User> updateInformation(User user) {
        //username是不能被更新的
        //email也要进行一个校验,校验新的email是不是已经存在,并且存在的email如果相同的话,不能是我们当前的这个用户的.
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());
        if (resultCount > 0) {
            return ServerResponse.createByErrorMsg("email已被使用,请更换email再尝试更新");
        }
        User updateUser = new User();
        BeanUtils.copyProperties(user, updateUser, "username", "password");

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if (updateCount > 0) {
            User data = userMapper.selectByPrimaryKey(updateUser.getId());
            data.setPassword(StringUtils.EMPTY);
            data.setAnswer(StringUtils.EMPTY);
            return ServerResponse.createBySuccess("更新个人信息成功", data);
        }
        return ServerResponse.createByErrorMsg("更新个人信息失败");
    }


    @Override
    public ServerResponse<User> getInfoById(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            return ServerResponse.createByErrorMsg("找不到当前用户");
        }
        // 若存在该用户，将返回的用户对象的密码属性置为空。
        user.setPassword(org.apache.commons.lang3.StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);

    }

    //backend
    @Override
    public boolean isAdminRole(User user) {
        return user.getRole().intValue() == Const.Role.ROLE_ADMIN;
    }

}