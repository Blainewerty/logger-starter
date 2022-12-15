package com.farzoom.persimmon.logger.starter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = LoggerConfiguration.class)
public class LoggerConfiguration {
}
