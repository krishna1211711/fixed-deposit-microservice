package com.bank.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class RateLimiterConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterConfig.class);

    public RateLimiterConfig() {
        logger.info("Simple In-Memory Rate Limiter Configured (Logging mode)");
    }
}
