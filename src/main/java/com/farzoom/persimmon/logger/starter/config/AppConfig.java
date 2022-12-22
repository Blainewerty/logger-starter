package com.farzoom.persimmon.logger.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "farzoom.logger")
public class AppConfig {

    private boolean headerDebugOn;
}
