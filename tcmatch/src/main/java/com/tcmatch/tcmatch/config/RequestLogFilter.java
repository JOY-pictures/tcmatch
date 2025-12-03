//package com.tcmatch.tcmatch.config;
//
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.core.annotation.Order;
//import jakarta.servlet.*;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//@Component
//@Slf4j
//public class RequestLogFilter implements Filter {
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//
//        log.info("🌐 Входящий запрос:");
//        log.info("  Метод: {}", httpRequest.getMethod());
//        log.info("  URL: {}", httpRequest.getRequestURL());
//        log.info("  Протокол: {}", httpRequest.getProtocol());
//        log.info("  Secure: {}", httpRequest.isSecure());
//        log.info("  Content-Type: {}", httpRequest.getContentType());
//
//        // Логируем заголовки Cloudpub
//        String forwardedProto = httpRequest.getHeader("X-Forwarded-Proto");
//        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
//        String host = httpRequest.getHeader("Host");
//
//        log.info("  X-Forwarded-Proto: {}", forwardedProto);
//        log.info("  X-Forwarded-For: {}", forwardedFor);
//        log.info("  Host: {}", host);
//
//        chain.doFilter(request, response);
//    }
//}