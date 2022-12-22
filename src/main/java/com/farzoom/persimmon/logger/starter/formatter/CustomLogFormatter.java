package com.farzoom.persimmon.logger.starter.formatter;

import com.farzoom.persimmon.logger.starter.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.HttpMessage;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;
import org.zalando.logbook.Precorrelation;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogFormatter implements HttpLogFormatter {

    private static final String ID = "id";
    private static final String URI = "\n      URI: ";
    private static final String BODY = "\n      BODY: ";
    private static final String STATUS = "\n      STATUS: ";
    private static final String REQ_CODE = "\n      REQ_CODE: ";
    private static final String DIRECTION_TO = "\n<<<<< ";
    private static final String DIRECTION_FROM = "\n>>>>> ";
    private static final String DEBUG_HEADER = "far-debug";

    private final AppConfig config;
    private final ObjectMapper mapper;


    @Override
    public String format(Precorrelation precorrelation, HttpRequest request) {
        Optional.ofNullable(request.getHeaders().get(DEBUG_HEADER))
                .ifPresent(value -> MDC.put(DEBUG_HEADER, value.get(0)));
        if (MDC.get(ID) == null) {
            MDC.put(ID, precorrelation.getId());
        }
        return direction(request)
                + request.getProtocolVersion() + ": " + request.getMethod() +
                REQ_CODE + MDC.get(ID) +
                URI + getHostPath(request) +
                getBody(request);
    }

    @SneakyThrows
    private String getBody(HttpRequest request) {
        Boolean isHeaderForDebug = getHeaderForDebug();
        return log.isDebugEnabled() ||
                isHeaderForDebug ?
                BODY + request.getBodyAsString() :
                "";
    }

    private String getHostPath(HttpRequest request) {
        return request.getRequestUri();
    }

    @Override
    public String format(Correlation correlation, HttpResponse response) {
        return direction(response)
                + response.getProtocolVersion() +
                REQ_CODE + MDC.get(ID) +
                STATUS + response.getStatus() + " " + response.getReasonPhrase() +
                getBody(response);
    }

    private String direction(final HttpMessage request) {
        return request.getOrigin() == Origin.REMOTE ? DIRECTION_TO : DIRECTION_FROM;
    }

    @SneakyThrows
    private String getBody(HttpResponse response) {
        Boolean isHeaderForDebug = getHeaderForDebug();
        return log.isDebugEnabled() || isHeaderForDebug ?
                BODY + getPrettyJson(response.getBodyAsString()) :
                "";
    }

    @SneakyThrows
    private String getPrettyJson(String body) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(body));
    }

    private Boolean getHeaderForDebug() {
        return config.isHeaderDebugOn() &&
                Optional.ofNullable(MDC.get(DEBUG_HEADER))
                        .map("true"::equals)
                        .orElse(false);
    }
}
