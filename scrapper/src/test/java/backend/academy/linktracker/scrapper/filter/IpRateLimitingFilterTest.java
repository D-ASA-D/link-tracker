package backend.academy.linktracker.scrapper.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IpRateLimitingFilterTest {

    @Test
    void shouldReturnTooManyRequestsWhenIpLimitExceeded() throws Exception {
        ResilienceProperties properties = new ResilienceProperties();
        properties.getRateLimit().setLimitForPeriod(2);
        properties.getRateLimit().setRefreshPeriod(Duration.ofMinutes(1));
        properties.getRateLimit().setTimeoutDuration(Duration.ZERO);

        IpRateLimitingFilter filter = new IpRateLimitingFilter(properties);
        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletRequest firstRequest = requestFromIp("10.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        MockHttpServletRequest secondRequest = requestFromIp("10.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        MockHttpServletRequest thirdRequest = requestFromIp("10.0.0.1");
        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();

        filter.doFilter(firstRequest, firstResponse, filterChain);
        filter.doFilter(secondRequest, secondResponse, filterChain);
        filter.doFilter(thirdRequest, thirdResponse, filterChain);

        verify(filterChain, times(2)).doFilter(any(), any());

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), thirdResponse.getStatus());
        assertEquals("application/json", thirdResponse.getContentType());
    }

    @Test
    void shouldUseXForwardedForAsClientIp() throws Exception {
        ResilienceProperties properties = new ResilienceProperties();
        properties.getRateLimit().setLimitForPeriod(1);
        properties.getRateLimit().setRefreshPeriod(Duration.ofMinutes(1));
        properties.getRateLimit().setTimeoutDuration(Duration.ZERO);

        IpRateLimitingFilter filter = new IpRateLimitingFilter(properties);
        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletRequest firstRequest = new MockHttpServletRequest();
        firstRequest.addHeader("X-Forwarded-For", "192.168.1.10, 192.168.1.11");

        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        secondRequest.addHeader("X-Forwarded-For", "192.168.1.10, 192.168.1.11");

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        filter.doFilter(firstRequest, firstResponse, filterChain);
        filter.doFilter(secondRequest, secondResponse, filterChain);

        verify(filterChain, times(1)).doFilter(any(), any());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), secondResponse.getStatus());
    }

    private MockHttpServletRequest requestFromIp(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        return request;
    }
}
