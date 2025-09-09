package main.java.main.java.com.example.inventoryservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Bean
    public Bucket createApiBucket() {
        // 100 requests per minute per IP
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    @Bean
    public Bucket createAdminBucket() {
        // 50 requests per minute for admin operations (more restrictive)
        Bandwidth limit = Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1)));
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    public Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, this::createApiBucket);
    }

    public boolean isRequestPermitted(String clientIp, String endpointType) {
        Bucket bucket = resolveBucket(clientIp + ":" + endpointType);
        return bucket.tryConsume(1);
    }

    public HttpStatus getRateLimitStatus(String clientIp, String endpointType) {
        Bucket bucket = resolveBucket(clientIp + ":" + endpointType);
        if (bucket.tryConsume(1)) {
            return HttpStatus.OK;
        }
        long waitTime = bucket.getAvailableTokens();
        if (waitTime > 0) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}