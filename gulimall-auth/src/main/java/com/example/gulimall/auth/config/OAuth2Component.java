package com.example.gulimall.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Date: 2022/10/18 17:50
 */

@ConfigurationProperties(prefix = "oauth2.gitee")
@Component
@Data
public class OAuth2Component {
    private String grantType;
    private String clientId;
    private String redirectUri;
    private String clientSecret;
}
