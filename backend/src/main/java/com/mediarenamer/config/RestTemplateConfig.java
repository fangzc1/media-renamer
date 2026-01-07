package com.mediarenamer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.*;

/**
 * RestTemplate 配置
 * 支持代理和超时配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final ProxyProperties proxyProperties;
    private final TmdbProperties tmdbProperties;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());

        String proxyInfo = proxyProperties.isEnabled() && proxyProperties.getHost() != null
            ? proxyProperties.getHost() + ":" + proxyProperties.getPort()
            : "未启用";

        int connectTimeout = tmdbProperties.getTimeout() != null
            ? tmdbProperties.getTimeout().getConnect()
            : 10000;

        int readTimeout = tmdbProperties.getTimeout() != null
            ? tmdbProperties.getTimeout().getRead()
            : 30000;

        log.info("RestTemplate 配置完成 - 代理: {}, 连接超时: {} ms, 读取超时: {} ms",
                proxyInfo, connectTimeout, readTimeout);

        return restTemplate;
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 配置超时
        if (tmdbProperties.getTimeout() != null) {
            factory.setConnectTimeout(tmdbProperties.getTimeout().getConnect());
            factory.setReadTimeout(tmdbProperties.getTimeout().getRead());
        } else {
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(30000);
        }

        // 配置代理
        if (proxyProperties.isEnabled() && proxyProperties.getHost() != null && !proxyProperties.getHost().isEmpty()) {
            configureProxy(factory);
        }

        return factory;
    }

    /**
     * 配置代理
     */
    private void configureProxy(SimpleClientHttpRequestFactory factory) {
        try {
            Proxy.Type proxyType = proxyProperties.getType() == ProxyProperties.ProxyType.SOCKS
                    ? Proxy.Type.SOCKS
                    : Proxy.Type.HTTP;

            InetSocketAddress proxyAddress = new InetSocketAddress(
                    proxyProperties.getHost(),
                    proxyProperties.getPort()
            );

            Proxy proxy = new Proxy(proxyType, proxyAddress);
            factory.setProxy(proxy);

            log.info("代理配置成功: {}://{}:{}",
                    proxyType.name(),
                    proxyProperties.getHost(),
                    proxyProperties.getPort());

            // 配置代理认证（如果需要）
            if (proxyProperties.needsAuthentication()) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestorType() == RequestorType.PROXY) {
                            return new PasswordAuthentication(
                                    proxyProperties.getUsername(),
                                    proxyProperties.getPassword().toCharArray()
                            );
                        }
                        return null;
                    }
                });
                log.info("代理认证已配置: 用户名={}", proxyProperties.getUsername());
            }

        } catch (Exception e) {
            log.error("配置代理失败", e);
            throw new RuntimeException("代理配置失败: " + e.getMessage(), e);
        }
    }
}

