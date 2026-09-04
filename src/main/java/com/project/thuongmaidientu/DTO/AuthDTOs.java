package com.project.thuongmaidientu.DTO;

import lombok.*;

public class AuthDTOs {

    @Data
    public static class RegisterRequest {
        private String fullName;
        private String email;
        private String password;
    }

    @Data
    public static class LoginRequest {
        private String fullName;
        private String email;
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String fullName;
        private String role;
    }
}