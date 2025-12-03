package com.tcmatch.tcmatch.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@Slf4j
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/ping")
    public Map<String, String> ping(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("scheme", request.getScheme());
        response.put("secure", String.valueOf(request.isSecure()));
        response.put("serverPort", String.valueOf(request.getServerPort()));
        response.put("x-forwarded-proto", request.getHeader("X-Forwarded-Proto"));
        return response;
    }

    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody String body, HttpServletRequest request) {
        log.info("📥 Получен POST запрос:");
        log.info("Тело: {}", body);

        Map<String, Object> response = new HashMap<>();
        response.put("received", true);
        response.put("body", body);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("headers", new HashMap<String, String>() {{
            put("X-Forwarded-Proto", request.getHeader("X-Forwarded-Proto"));
            put("Content-Type", request.getContentType());
        }});

        return response;
    }
}