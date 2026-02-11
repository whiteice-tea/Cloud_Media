package com.cloudmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.cloudmedia.config.JwtUtil;
import com.cloudmedia.mapper.UserMapper;
import com.cloudmedia.model.dto.LoginRequest;
import com.cloudmedia.model.dto.RegisterRequest;
import com.cloudmedia.model.entity.User;
import com.cloudmedia.model.vo.LoginResponse;
import com.cloudmedia.model.vo.UserInfoVO;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public UserInfoVO register(RegisterRequest request) {
        String username = request.getUsername().trim();

        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new ApiException(ApiCode.BAD_REQUEST, "username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        int rows = userMapper.insert(user);
        if (rows != 1) {
            throw new ApiException(ApiCode.INTERNAL_ERROR, "failed to create user");
        }
        return new UserInfoVO(user.getId(), user.getUsername(), user.getEmail());
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(ApiCode.UNAUTHORIZED, "username or password incorrect");
        }

        String token = jwtUtil.createToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getUsername());
    }

    public UserInfoVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ApiCode.NOT_FOUND, "user not found");
        }
        return new UserInfoVO(user.getId(), user.getUsername(), user.getEmail());
    }
}
