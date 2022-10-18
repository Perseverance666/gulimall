package com.example.gulimall.auth.vo;

import lombok.Data;

/**
 * @Date: 2022/10/18 18:07
 */

@Data
public class SocialUser {

    private String access_token;
    private String token_type;
    private long expires_in;
    private String refresh_token;
    private String scope;
    private String created_at;
}
