package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Configuration
@EnableConfigurationProperties(ResilienceProperties.class)
public class ResilienceConfiguration {

    @Bean
    public Retry externalHttpRetry(ResilienceProperties properties) {
        ResilienceProperties.Retry retryProperties = properties.getRetry();

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retryProperties.getMaxAttempts())
                .retryOnException(isRetryableException(properties))
                .intervalFunction(createIntervalFunction(retryProperties))
                .build();

        return Retry.of("externalHttpRetry", config);
    }

    @Bean
    public CircuitBreaker externalHttpCircuitBreaker(ResilienceProperties properties) {
        ResilienceProperties.CircuitBreaker circuitBreakerProperties = properties.getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(circuitBreakerProperties.getSlidingWindowSize())
                .minimumNumberOfCalls(circuitBreakerProperties.getMinimumNumberOfCalls())
                .failureRateThreshold(circuitBreakerProperties.getFailureRateThreshold())
                .permittedNumberOfCallsInHalfOpenState(circuitBreakerProperties.getPermittedCallsInHalfOpenState())
                .waitDurationInOpenState(circuitBreakerProperties.getWaitDurationInOpenState())
                .recordException(isCircuitBreakerFailure())
                .build();

        return CircuitBreaker.of("externalHttpCircuitBreaker", config);
    }

    private Predicate<Throwable> isRetryableException(ResilienceProperties properties) {
        return throwable -> {
            Throwable current = unwrap(throwable);

            if (current instanceof ResourceAccessException) {
                return true;
            }

            if (current instanceof HttpStatusCodeException exception) {
                int statusCode = exception.getStatusCode().value();
                return properties.getRetry().getRetryableStatuses().contains(statusCode);
            }

            return false;
        };
    }

    private Predicate<Throwable> isCircuitBreakerFailure() {
        return throwable -> {
            Throwable current = unwrap(throwable);

            if (current instanceof IllegalArgumentException) {
                return false;
            }

            return current instanceof ResourceAccessException
                    || current instanceof HttpStatusCodeException
                    || current instanceof IllegalStateException;
        };
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        return current;
    }

    private IntervalFunction createIntervalFunction(ResilienceProperties.Retry retryProperties) {
        if (retryProperties.getBackoffStrategy() == ResilienceProperties.BackoffStrategy.EXPONENTIAL) {
            log.info(
                    "external_http_retry_backoff_strategy strategy={} waitDuration={} multiplier={}",
                    retryProperties.getBackoffStrategy(),
                    retryProperties.getWaitDuration(),
                    retryProperties.getExponentialMultiplier());

            return IntervalFunction.ofExponentialBackoff(
                    retryProperties.getWaitDuration(), retryProperties.getExponentialMultiplier());
        }

        log.info(
                "external_http_retry_backoff_strategy strategy={} waitDuration={}",
                retryProperties.getBackoffStrategy(),
                retryProperties.getWaitDuration());

        return IntervalFunction.of(retryProperties.getWaitDuration());
    }
}
