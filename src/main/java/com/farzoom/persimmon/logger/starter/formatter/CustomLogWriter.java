package com.farzoom.persimmon.logger.starter.formatter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.Precorrelation;

import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "farzoom.logger", name = "server.enable", havingValue = "true", matchIfMissing = true)
public class CustomLogWriter implements HttpLogWriter {

    @Override
    public void write(Precorrelation precorrelation, String request) throws IOException {
        log.info(request);
    }

    @Override
    public void write(Correlation correlation, String response) throws IOException {
        log.info(response);
    }
}
