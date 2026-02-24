package com.ecommerce.user.service;

import com.ecommerce.user.dto.UserLoginDTO;
import com.ecommerce.user.dto.UserRegisterDTO;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.vo.UserLoginVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    void register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     */
    UserLoginVO login(UserLoginDTO loginDTO);

    /**
     * 根据ID查询用户
     */
    User getUserById(Long userId);

    /**
     * 根据用户名查询用户
     */
    User getUserByUsername(String username);

    /**
     * 登出
     */
    void logout(Long userId);
}
