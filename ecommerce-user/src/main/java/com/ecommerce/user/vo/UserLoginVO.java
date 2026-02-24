package com.ecommerce.user.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户登录响应VO
 */
@Data
public class UserLoginVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String token;
    private Long expireTime;

    public UserLoginVO(Long userId, String username, String nickname, String token) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.token = token;
        this.expireTime = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000; // 7天
    }
}
