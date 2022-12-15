package com.farzoom.persimmon.logger.starter.feignlogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.Request;
import feign.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Primary
@Component
public class CustomFeignLogger extends Logger {

    private static final String DEBUG_HEADER = "far-debug";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void log(String s, String s1, Object... objects) {
    }

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        Optional.ofNullable(request.headers().get(DEBUG_HEADER))
                .ifPresent(value -> MDC.put(DEBUG_HEADER, value.stream().findAny().orElse("")));
        String logString = "\n>>>>> "
                + "HTTP/1.1" + ": " + request.httpMethod() +
                "\n      URI: " + request.url() +
                getBody(request);
        log.info(logString);
    }

    @SneakyThrows
    private String getBody(Request request) {
        Boolean isHeaderForDebug = getHeaderForDebug();
        return log.isDebugEnabled() ||
                isHeaderForDebug ?
                "\n      BODY: " + getPrettyJson(request.body()) :
                "";
    }

    @SneakyThrows
    private String getPrettyJson(byte[] body) {
        if (body == null) return "";
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(body));
    }

    private Boolean getHeaderForDebug() {
        return Optional.ofNullable(MDC.get(DEBUG_HEADER))
                .map("true"::equals)
                .orElse(false);
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) throws IOException {
        BufferedReader br = new BufferedReader(response.body().asReader(StandardCharsets.UTF_8));

        String prettyJson = getPrettyJson(br);
        String body = getBody(prettyJson);
        String logString = "\n<<<<< "
                + "HTTP/1.1" +
                "\n      STATUS: " + response.status() + " " + response.reason() +
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
    private String getPrettyJson(BufferedReader bufferedReader) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(bufferedReader));
    }

    @SneakyThrows
    private String getBody(String prettyJson) {
        Boolean isHeaderForDebug = getHeaderForDebug();
        return log.isDebugEnabled() || isHeaderForDebug ?
                "\n      BODY: " + prettyJson :
                "";
    }
}