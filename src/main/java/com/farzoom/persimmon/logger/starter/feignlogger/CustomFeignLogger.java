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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("${farzoom.logger.feign.enable:true} and '${feign.client.config.default.loggerLevel}'.equals('BASIC')")
public class CustomFeignLogger extends Logger {

    private static final String ID = "id";
    private static final String INDENT = "\n      ";
    private static final String URI = INDENT + "URI: ";
    private static final String HEADERS = INDENT + "HEADERS: ";
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
                HEADERS + request.headers() +
                URI + request.url() +
                getBody(request);
        log.info(logString);
    }

    @SneakyThrows
    private String getBody(Request request) {
        return needForBody() ?
                BODY + getPrettyJson(request.body()) :
                "";
    }

    @SneakyThrows
    private String getPrettyJson(byte[] body) {
        if (body == null) return "";
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(body));
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) {
        String logString = DIRECTION_TO +
                "HTTP/1.1" +
                REQ_CODE + MDC.get(ID) +
                HEADERS + response.headers() +
                URI + response.request().url() +
                STATUS + response.status() + " " + response.reason();

        if (needForBody()) {
            BufferedReader br = new BufferedReader(getReader(response));

            String prettyJson = getPrettyJson(br);
            String body = getBody(prettyJson);
            log.info(logString + body);

            InputStream inputStream = getInputStream(prettyJson);
            return Response.builder()
                    .request(response.request())
                    .reason(response.reason())
                    .status(response.status())
                    .headers(response.headers())
                    .body(inputStream, Optional.ofNullable(response.body())
                            .map(Response.Body::length)
                            .orElse(0))
                    .build();
        } else {
            log.info(logString);
            return response;
        }
    }

    private InputStream getInputStream(String prettyJson) {
        return "null".equals(prettyJson) ?
                InputStream.nullInputStream() :
                new ByteArrayInputStream(prettyJson.getBytes());
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
        return needForBody() ?
                BODY + prettyJson :
                "";
    }

    private boolean needForBody() {
        return log.isDebugEnabled() || getHeaderForDebug();
    }

    private Boolean getHeaderForDebug() {
        return config.isHeaderDebugOn() &&
                Optional.ofNullable(MDC.get(DEBUG_HEADER))
                        .map("true"::equals)
                        .orElse(false);

    }
}