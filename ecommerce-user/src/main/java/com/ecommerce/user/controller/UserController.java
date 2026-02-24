package com.ecommerce.user.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.common.utils.JwtUtil;
import com.ecommerce.user.dto.UserLoginDTO;
import com.ecommerce.user.dto.UserRegisterDTO;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import com.ecommerce.user.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Api(tags = "用户管理")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Result<Void> register(@Validated @RequestBody UserRegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功", null);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<UserLoginVO> login(@Validated @RequestBody UserLoginDTO loginDTO) {
        UserLoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @ApiOperation("获取当前用户信息")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String token) {
        Long userId = JwtUtil.getUserId(token);
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @ApiOperation("用户登出")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        Long userId = JwtUtil.getUserId(token);
        userService.logout(userId);
        return Result.success("登出成功", null);
    }

    /**
     * 根据ID查询用户（内部调用）
     */
    @GetMapping("/internal/{userId}")
    @ApiOperation(value = "根据ID查询用户", hidden = true)
    public Result<User> getUserByIdInternal(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }
}
