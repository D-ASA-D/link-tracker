package backend.academy.linktracker.scrapper.metrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ScrapperApiMetricsFilter extends OncePerRequestFilter {

    private static final String SERVICE_NAME_HEADER = "X-Service-Name";
    private static final String METRICS_PATH = "/metrics";
    private static final String HEALTH_PATH = "/health";

    private final ScrapperMetricsService metricsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        metricsService.incrementApiRequest(resolveSource(request));
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return METRICS_PATH.equals(uri) || HEALTH_PATH.equals(uri);
    }

    private String resolveSource(HttpServletRequest request) {
        String serviceName = request.getHeader(SERVICE_NAME_HEADER);

        if (serviceName != null && !serviceName.isBlank()) {
            return serviceName;
        }

        return "unknown";
    }
}
