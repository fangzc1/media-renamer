package com.mediarenamer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 代理配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "proxy")
public class ProxyProperties {

    /**
     * 是否启用代理
     */
    private boolean enabled = false;

    /**
     * 代理主机
     */
    private String host;

    /**
     * 代理端口
     */
    private Integer port = 7890;

    /**
     * 代理类型: HTTP, SOCKS
     */
    private ProxyType type = ProxyType.HTTP;

    /**
     * 代理用户名（可选）
     */
    private String username;

    /**
     * 代理密码（可选）
     */
    private String password;

    /**
     * 代理类型枚举
     */
    public enum ProxyType {
        HTTP,
        SOCKS
    }

    /**
     * 是否需要认证
     */
    public boolean needsAuthentication() {
        return username != null && !username.isEmpty()
            && password != null && !password.isEmpty();
    }
}
