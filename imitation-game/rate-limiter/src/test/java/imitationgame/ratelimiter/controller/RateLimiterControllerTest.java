package imitationgame.ratelimiter.controller;

import imitationgame.ratelimiter.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimiterControllerTest {

    private RateLimiterService rateLimiterService;
    private RateLimiterController controller;

    @BeforeEach
    void setUp() {
        rateLimiterService = Mockito.mock(RateLimiterService.class);
        controller = new RateLimiterController();
        try {
            java.lang.reflect.Field field = RateLimiterController.class.getDeclaredField("rateLimiterService");
            field.setAccessible(true);
            field.set(controller, rateLimiterService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RateLimiterService.RateLimitResult allowed() {
        return new RateLimiterService.RateLimitResult(true, 5, 10);
    }

    @Test
    void check_ok() {
        Mockito.when(rateLimiterService.checkRateLimit("u1")).thenReturn(allowed());
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1").build();
        assertEquals(HttpStatus.OK, controller.checkRateLimit(jwt).getStatusCode());
    }

    @Test
    void checkForUser_ok() {
        Mockito.when(rateLimiterService.checkRateLimit("u2")).thenReturn(allowed());
        assertEquals(HttpStatus.OK, controller.checkRateLimitForUser("u2").getStatusCode());
    }

    @Test
    void status_ok() {
        Mockito.when(rateLimiterService.getRequestCount("u1")).thenReturn(1);
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1").build();
        assertEquals(HttpStatus.OK, controller.getStatus(jwt).getStatusCode());
    }

    @Test
    void reset_ok() {
        assertEquals(HttpStatus.OK, controller.resetRateLimit("u3").getStatusCode());
    }

    @Test
    void health_ok() {
        assertEquals(HttpStatus.OK, controller.health().getStatusCode());
    }

    @Test
    void internalCheck_ok() {
        Mockito.when(rateLimiterService.checkRateLimit("u4")).thenReturn(allowed());
        assertEquals(HttpStatus.OK, controller.internalCheckRateLimit("u4").getStatusCode());
    }
}
