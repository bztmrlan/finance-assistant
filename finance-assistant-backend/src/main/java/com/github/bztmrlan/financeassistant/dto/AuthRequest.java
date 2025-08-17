package com.github.bztmrlan.financeassistant.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
}
