package com.cloudmedia.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cloudmedia.model.dto.LoginRequest;
import com.cloudmedia.model.dto.RegisterRequest;
import com.cloudmedia.model.vo.LoginResponse;
import com.cloudmedia.model.vo.UserInfoVO;
import com.cloudmedia.service.AuthService;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;
import com.cloudmedia.util.ApiResponse;
import com.cloudmedia.util.UserContext;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<UserInfoVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoVO> me() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ApiException(ApiCode.UNAUTHORIZED, "unauthorized");
        }
        return ApiResponse.ok(authService.me(userId));
    }
}
