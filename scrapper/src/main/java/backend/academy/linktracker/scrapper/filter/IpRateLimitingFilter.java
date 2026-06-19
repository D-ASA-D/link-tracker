package backend.academy.linktracker.scrapper.filter;

import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class IpRateLimitingFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String IP_LIST_SEPARATOR = ",";
    private static final String RATE_LIMITER_NAME_PREFIX = "ip-rate-limiter-";
    private static final String TOO_MANY_REQUESTS_RESPONSE = """
            {
              "description": "Too many requests",
              "code": "429",
              "exceptionName": "RateLimitExceededException",
              "exceptionMessage": "Too many requests",
              "stacktrace": []
            }
            """;

    private final ResilienceProperties properties;
    private final Map<String, RateLimiter> limitersByIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = resolveIp(request);
        RateLimiter limiter = limitersByIp.computeIfAbsent(ip, this::createRateLimiter);

        if (!limiter.acquirePermission()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(TOO_MANY_REQUESTS_RESPONSE);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimiter createRateLimiter(String ip) {
        ResilienceProperties.RateLimit rateLimitProperties = properties.getRateLimit();

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(rateLimitProperties.getLimitForPeriod())
                .limitRefreshPeriod(rateLimitProperties.getRefreshPeriod())
                .timeoutDuration(rateLimitProperties.getTimeoutDuration())
                .build();

        return RateLimiter.of(RATE_LIMITER_NAME_PREFIX + ip, config);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(IP_LIST_SEPARATOR)[0].trim();
        }

        return request.getRemoteAddr();
    }
}
