package com.finpay.common.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.finpay.common.ai.AiAuthenticationException;
import com.finpay.common.ai.AiException;
import com.finpay.common.ai.AiRateLimitException;
import com.finpay.common.ai.AiRequest;
import com.finpay.common.ai.AiResponse;
import com.finpay.common.ai.AiServiceException;
import com.finpay.common.ai.AiTimeoutException;
import com.finpay.common.ai.ApiKeyProvider;
import com.finpay.common.ai.SecretRedactor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

class HttpAiClientTest {

    private static final String KEY = "sk-test-super-secret-1234567890";
    private static final String BASE_URL = "https://ai.finpay.test";
    private static final String ENDPOINT = BASE_URL + "/chat/completions";
    private static final AiRequest REQUEST = new AiRequest("req-1", "openai", "gpt-test", "Explain idempotency");

    private MockRestServiceServer server;
    private HttpAiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpAiClient(builder.baseUrl(BASE_URL).build(), new FixedKeyProvider(KEY));
    }

    @Test
    void sends_the_bearer_key_in_the_authorization_header_and_returns_the_response() {
        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + KEY))
                .andRespond(withSuccess(
                        """
                        {"choices":[{"index":0,"message":{"role":"assistant","content":"Idempotency keys prevent duplicates"}}],"model":"gpt-test"}
                        """,
                        MediaType.APPLICATION_JSON));

        AiResponse response = client.complete(REQUEST);

        assertThat(response.content()).isEqualTo("Idempotency keys prevent duplicates");
        assertThat(response.requestId()).isEqualTo(REQUEST.requestId());
        assertThat(response.latencyMillis()).isGreaterThanOrEqualTo(0);
        server.verify();
    }

    @Test
    void maps_401_to_authentication_without_leaking_the_key() {
        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + KEY))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        AiException error = catchThrowableOfType(() -> client.complete(REQUEST), AiException.class);

        assertThat(error).isInstanceOf(AiAuthenticationException.class);
        assertThat(error.code()).isEqualTo("AI_AUTHENTICATION");
        assertThat(error.getMessage()).doesNotContain(KEY);
        assertThat(error.getMessage()).doesNotContain(SecretRedactor.MASKED_UNSET);
    }

    @Test
    void maps_429_to_rate_limit() {
        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + KEY))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        AiException error = catchThrowableOfType(() -> client.complete(REQUEST), AiException.class);

        assertThat(error).isInstanceOf(AiRateLimitException.class);
        assertThat(error.code()).isEqualTo("AI_RATE_LIMITED");
        assertThat(error.retryable()).isTrue();
    }

    @Test
    void maps_5xx_to_service_error() {
        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + KEY))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        AiException error = catchThrowableOfType(() -> client.complete(REQUEST), AiException.class);

        assertThat(error).isInstanceOf(AiServiceException.class);
        assertThat(error.code()).isEqualTo("AI_SERVICE_ERROR");
    }

    @Test
    void maps_transport_failure_to_timeout() {
        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + KEY))
                .andRespond(withException(new SocketTimeoutException("read timed out")));

        AiException error = catchThrowableOfType(() -> client.complete(REQUEST), AiException.class);

        assertThat(error).isInstanceOf(AiTimeoutException.class);
        assertThat(error.code()).isEqualTo("AI_TIMEOUT");
        assertThat(error.retryable()).isTrue();
        assertThat(error.getMessage()).doesNotContain(KEY);
    }

    private static final class FixedKeyProvider implements ApiKeyProvider {
        private final String key;

        FixedKeyProvider(String key) {
            this.key = key;
        }

        @Override
        public String get() {
            return key;
        }
    }
}