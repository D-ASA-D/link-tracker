package backend.academy.linktracker.scrapper.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResilientHttpExecutor {

    private final Retry externalHttpRetry;
    private final CircuitBreaker externalHttpCircuitBreaker;

    public <T> T execute(Supplier<T> supplier) {
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(
                externalHttpCircuitBreaker, Retry.decorateSupplier(externalHttpRetry, supplier));

        return decoratedSupplier.get();
    }

    public void executeVoid(Runnable runnable) {
        Runnable decoratedRunnable = CircuitBreaker.decorateRunnable(
                externalHttpCircuitBreaker, Retry.decorateRunnable(externalHttpRetry, runnable));

        decoratedRunnable.run();
    }
}
