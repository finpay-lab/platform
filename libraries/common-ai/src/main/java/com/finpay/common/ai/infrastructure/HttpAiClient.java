package com.finpay.common.ai.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.finpay.common.ai.AiAuthenticationException;
import com.finpay.common.ai.AIClient;
import com.finpay.common.ai.AiException;
import com.finpay.common.ai.AiRateLimitException;
import com.finpay.common.ai.AiRequest;
import com.finpay.common.ai.AiResponse;
import com.finpay.common.ai.AiServiceException;
import com.finpay.common.ai.AiTimeoutException;
import com.finpay.common.ai.ApiKeyProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI-compatible BYOK HTTP transport for {@link AIClient}. The API key is
 * read from the {@link ApiKeyProvider} per call, placed only in the
 * Authorization header, and never included in exceptions or audit data.
 * HTTP statuses map onto the {@code com.finpay.common.ai} exception hierarchy
 * (401/403 auth, 429 rate limit, 5xx service, transport failures timeout).
 */
public final class HttpAiClient implements AIClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final RestClient restClient;
    private final ApiKeyProvider apiKeyProvider;

    /**
     * Accepts a fully configured {@link RestClient} (timeouts are a wiring
     * concern, configured via the convenience constructor or the caller's own
     * request factory). Separated so tests can inject a mocked transport.
     */
    public HttpAiClient(RestClient restClient, ApiKeyProvider apiKeyProvider) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.apiKeyProvider = Objects.requireNonNull(apiKeyProvider, "apiKeyProvider");
    }

    public HttpAiClient(
            RestClient.Builder builder,
            String baseUrl,
            ApiKeyProvider apiKeyProvider,
            Duration connectTimeout,
            Duration readTimeout) {
        this(buildRestClient(builder, baseUrl, connectTimeout, readTimeout), apiKeyProvider);
    }

    private static RestClient buildRestClient(
            RestClient.Builder builder, String baseUrl, Duration connectTimeout, Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        Objects.requireNonNull(readTimeout, "readTimeout");
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());
        return builder.requestFactory(requestFactory).baseUrl(baseUrl).build();
    }

    @Override
    public AiResponse complete(AiRequest request) {
        long startedAt = System.nanoTime();
        String apiKey = apiKeyProvider.get();
        ChatCompletionRequest body =
                new ChatCompletionRequest(request.model(), List.of(new ChatMessage("user", request.prompt())));
        ResponseEntity<ChatCompletionResponse> entity;
        try {
            entity = restClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(ChatCompletionResponse.class);
        } catch (RestClientResponseException e) {
            throw mapHttpError(e);
        } catch (ResourceAccessException e) {
            throw new AiTimeoutException("AI provider not reachable: " + e.getMessage(), e);
        }
        ChatCompletionResponse response = entity.getBody();
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new AiServiceException("AI provider returned an empty response body");
        }
        String content = response.choices().get(0).message().content();
        long latencyMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        return new AiResponse(request.requestId(), request.model(), content, latencyMillis);
    }

    private static AiException mapHttpError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 401 || status == 403) {
            return new AiAuthenticationException("AI provider rejected credentials (HTTP " + status + ")", e);
        }
        if (status == 429) {
            return new AiRateLimitException("AI provider rate limited (HTTP 429)", e);
        }
        return new AiServiceException("AI provider error (HTTP " + status + ")", e);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatCompletionRequest(String model, List<ChatMessage> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatCompletionResponse(List<Choice> choices, String model) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Choice(int index, ChatMessage message) {
        }
    }
}