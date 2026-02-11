package com.cloudmedia.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "must not be blank")
    @Size(min = 3, max = 64, message = "length must be between 3 and 64")
    private String username;

    @NotBlank(message = "must not be blank")
    @Size(min = 6, max = 64, message = "length must be between 6 and 64")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
