package backend.academy.linktracker.scrapper.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import backend.academy.linktracker.scrapper.configuration.ResilienceConfiguration;
import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class ResilientHttpCircuitBreakerTest {

    private static final String OK_RESPONSE = "ok";
    private static final Duration WAIT_DURATION_IN_OPEN_STATE = Duration.ofMillis(500);
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(3);

    @Test
    void shouldOpenCircuitBreakerAfterFailureRateThresholdExceeded() {
        TestContext context = createContext(2);

        openCircuitBreaker(context.executor);

        assertEquals(CircuitBreaker.State.OPEN, context.circuitBreaker.getState());

        assertThrows(CallNotPermittedException.class, () -> context.executor.execute(() -> OK_RESPONSE));
    }

    @Test
    void shouldMoveFromHalfOpenToClosedWhenTrialCallsAreSuccessful() {
        TestContext context = createContext(2);

        openCircuitBreaker(context.executor);

        assertEquals(CircuitBreaker.State.OPEN, context.circuitBreaker.getState());

        Awaitility.await()
                .ignoreExceptionsInstanceOf(CallNotPermittedException.class)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> assertEquals(OK_RESPONSE, context.executor.execute(() -> OK_RESPONSE)));

        assertEquals(OK_RESPONSE, context.executor.execute(() -> OK_RESPONSE));

        Awaitility.await()
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> assertEquals(CircuitBreaker.State.CLOSED, context.circuitBreaker.getState()));
    }

    @Test
    void shouldMoveFromHalfOpenBackToOpenWhenTrialCallsFail() {
        TestContext context = createContext(1);

        openCircuitBreaker(context.executor);

        assertEquals(CircuitBreaker.State.OPEN, context.circuitBreaker.getState());

        Awaitility.await()
                .ignoreExceptionsInstanceOf(CallNotPermittedException.class)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> assertThrows(
                        ResourceAccessException.class,
                        () -> context.executor.execute(() -> {
                            throw new ResourceAccessException("trial failed");
                        })));

        assertEquals(CircuitBreaker.State.OPEN, context.circuitBreaker.getState());

        assertThrows(CallNotPermittedException.class, () -> context.executor.execute(() -> OK_RESPONSE));
    }

    private void openCircuitBreaker(ResilientHttpExecutor executor) {
        for (int i = 0; i < 5; i++) {
            try {
                executor.execute(() -> {
                    throw new ResourceAccessException("external service failed");
                });
            } catch (ResourceAccessException ignored) {
                // Expected while opening circuit breaker.
            }
        }
    }

    private TestContext createContext(int permittedCallsInHalfOpenState) {
        ResilienceProperties properties = new ResilienceProperties();

        properties.getRetry().setMaxAttempts(1);
        properties.getRetry().setWaitDuration(Duration.ofMillis(1));

        properties.getCircuitBreaker().setSlidingWindowSize(5);
        properties.getCircuitBreaker().setMinimumNumberOfCalls(5);
        properties.getCircuitBreaker().setFailureRateThreshold(50);
        properties.getCircuitBreaker().setPermittedCallsInHalfOpenState(permittedCallsInHalfOpenState);
        properties.getCircuitBreaker().setWaitDurationInOpenState(WAIT_DURATION_IN_OPEN_STATE);

        ResilienceConfiguration configuration = new ResilienceConfiguration();

        Retry retry = configuration.externalHttpRetry(properties);
        CircuitBreaker circuitBreaker = configuration.externalHttpCircuitBreaker(properties);
        ResilientHttpExecutor executor = new ResilientHttpExecutor(retry, circuitBreaker);

        return new TestContext(executor, circuitBreaker);
    }

    private record TestContext(ResilientHttpExecutor executor, CircuitBreaker circuitBreaker) {}
}
