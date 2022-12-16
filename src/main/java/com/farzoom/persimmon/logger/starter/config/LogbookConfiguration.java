package com.farzoom.persimmon.logger.starter.config;

import com.farzoom.persimmon.logger.starter.formatter.CustomLogFormatter;
import com.farzoom.persimmon.logger.starter.formatter.CustomLogWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.DefaultSink;
import org.zalando.logbook.Logbook;

import static org.zalando.logbook.Conditions.exclude;
import static org.zalando.logbook.Conditions.requestTo;

@Configuration
@RequiredArgsConstructor
public class LogbookConfiguration {

    private final CustomLogWriter logWriter;
    private final CustomLogFormatter logFormatter;

    @Bean
    public Logbook logbook() {
        return Logbook.builder()
                .condition(exclude(
                        requestTo("/swagger-resources/**"),
                        requestTo("/swagger-ui/**"),
                        requestTo("/actuator/**"),
                        requestTo("/v2/**"),
                        requestTo("/v3/**")))
                .sink(new DefaultSink(
                        logFormatter,
                        logWriter)
                )
                .build();
    }
}
