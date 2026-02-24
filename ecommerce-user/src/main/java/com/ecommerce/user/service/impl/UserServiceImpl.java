package com.ecommerce.user.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResultCode;
import com.ecommerce.common.utils.JwtUtil;
import com.ecommerce.user.dto.UserLoginDTO;
import com.ecommerce.user.dto.UserRegisterDTO;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.service.UserService;
import com.ecommerce.user.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void register(UserRegisterDTO registerDTO) {
        // 检查用户名是否存在
        User existUser = getUserByUsername(registerDTO.getUsername());
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXIST);
        }

        // 创建用户
        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        
        // 密码加密（MD5）
        user.setPassword(SecureUtil.md5(registerDTO.getPassword()));
        user.setStatus(1);
        user.setGender(0);
        
        // 如果没有昵称，使用用户名
        if (registerDTO.getNickname() == null || registerDTO.getNickname().isEmpty()) {
            user.setNickname(registerDTO.getUsername());
        }

        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new BusinessException("注册失败");
        }
        
        log.info("用户注册成功：{}", user.getUsername());
    }

    @Override
    public UserLoginVO login(UserLoginDTO loginDTO) {
        // 查询用户
        User user = getUserByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 验证密码
        String encryptedPassword = SecureUtil.md5(loginDTO.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        // 生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        // 缓存Token到Redis（7天）
        String tokenKey = RedisKeyConstant.buildUserTokenKey(user.getId());
        redisTemplate.opsForValue().set(tokenKey, token, 7, TimeUnit.DAYS);

        // 缓存用户信息到Redis（1小时）
        String userKey = RedisKeyConstant.USER_INFO_PREFIX + user.getId();
        user.setPassword(null); // 清除密码
        redisTemplate.opsForValue().set(userKey, user, 1, TimeUnit.HOURS);

        log.info("用户登录成功：{}", user.getUsername());

        return new UserLoginVO(user.getId(), user.getUsername(), user.getNickname(), token);
    }

    @Override
    public User getUserById(Long userId) {
        // 先从Redis查询
        String userKey = RedisKeyConstant.USER_INFO_PREFIX + userId;
        User user = (User) redisTemplate.opsForValue().get(userKey);
        
        if (user == null) {
            // Redis没有，从数据库查询
            user = userMapper.selectById(userId);
            if (user != null) {
                user.setPassword(null);
                // 缓存到Redis（1小时）
                redisTemplate.opsForValue().set(userKey, user, 1, TimeUnit.HOURS);
            }
        }
        
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public void logout(Long userId) {
        // 删除Redis中的Token
        String tokenKey = RedisKeyConstant.buildUserTokenKey(userId);
        redisTemplate.delete(tokenKey);
        
        log.info("用户登出：{}", userId);
    }
}
