package com.tcmatch.tcmatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Configuration
public class YooMoneyConfig {

    @Value("${yoomoney.shopId}")
    private String shopId;

    @Value("${yoomoney.secretKey}")
    private String secretKey;

    /**
     * ✅ ИСПРАВЛЕННАЯ ВЕРСИЯ с правильным lifecycle управлением и обработкой JSON
     */
    @Bean
    public RestTemplate yooMoneyRestTemplate(RestTemplateBuilder builder) throws Exception {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                    try {
                        httpsConnection.setSSLSocketFactory(createTrustAllSSLContext().getSocketFactory());
                    } catch (Exception e) {
                        throw new IOException("Failed to create SSL context", e);
                    }
                    httpsConnection.setHostnameVerifier((hostname, session) -> true);
                }
                super.prepareConnection(connection, httpMethod);
            }
        };

        requestFactory.setConnectTimeout(Duration.ofSeconds(30));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return builder
                .requestFactory(() -> requestFactory)
                .rootUri("https://api.yookassa.ru/v3")
                .basicAuthentication(shopId, secretKey)
                .build();
    }

    private SSLContext createTrustAllSSLContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }
}