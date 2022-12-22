package com.farzoom.persimmon.logger.starter.feignlogger;

import com.farzoom.persimmon.logger.starter.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.Request;
import feign.Response;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class CustomFeignLogger extends Logger {

    private static final String ID = "id";
    private static final String INDENT = "\n      ";
    private static final String URI = INDENT + "URI: ";
    private static final String BODY = INDENT + "BODY: ";
    private static final String STATUS = INDENT + "STATUS: ";
    private static final String REQ_CODE = INDENT + "REQ_CODE: ";
    private static final String DIRECTION_TO = "\n<<<<< ";
    private static final String DIRECTION_FROM = "\n>>>>> ";
    private static final String DEBUG_HEADER = "far-debug";

    private final AppConfig config;
    private final ObjectMapper mapper;


    @Override
    protected void log(String s, String s1, Object... objects) {
    }

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        Optional.ofNullable(request.headers().get(DEBUG_HEADER))
                .ifPresent(value -> MDC.put(DEBUG_HEADER, value.stream().findAny().orElse("")));
        String logString = DIRECTION_FROM +
                "HTTP/1.1" + ": " + request.httpMethod() +
                REQ_CODE + MDC.get(ID) +
                URI + request.url() +
                getBody(request);
        log.info(logString);
    }

    @SneakyThrows
    private String getBody(Request request) {
        Boolean isHeaderForDebug = getHeaderForDebug();
        return log.isDebugEnabled() ||
                isHeaderForDebug ?
                BODY + getPrettyJson(request.body()) :
                "";
    }

    @SneakyThrows
    private String getPrettyJson(byte[] body) {
        if (body == null) return "";
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(body));
    }

    private Boolean getHeaderForDebug() {
        return config.isHeaderDebugOn() &&
                Optional.ofNullable(MDC.get(DEBUG_HEADER))
                        .map("true"::equals)
                        .orElse(false);

    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) {
        BufferedReader br = new BufferedReader(getReader(response));

        String prettyJson = getPrettyJson(br);
        String body = getBody(prettyJson);
        String logString = DIRECTION_TO +
                "HTTP/1.1" +
                REQ_CODE + MDC.get(ID) +
                URI + response.request().url() +
                STATUS + response.status() + " " + response.reason() +
                body;

        log.info(logString);
        return Response.builder()
                .request(response.request())
                .reason(response.reason())
                .status(response.status())
                .headers(response.headers())
                .body(prettyJson, StandardCharsets.UTF_8)
                .build();
    }

    @SneakyThrows
    private Reader getReader(Response response) {
        return response.body() != null ? new BufferedReader(response.body().asReader(StandardCharsets.UTF_8)) :
                Reader.nullReader();
    }

    private String getPrettyJson(BufferedReader bufferedReader) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(bufferedReader));
        } catch (IOException e) {
            log.error("Some problems with JSON parse");
            return "";
        }
    }

    @SneakyThrows
    private String getBody(String prettyJson) {
        Boolean isHeaderForDebug = getHeaderForDebug();
        return log.isDebugEnabled() || isHeaderForDebug ?
                BODY + prettyJson :
                "";
    }
}