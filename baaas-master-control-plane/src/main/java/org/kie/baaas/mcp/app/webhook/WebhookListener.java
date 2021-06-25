package org.kie.baaas.mcp.app.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.kie.baaas.mcp.app.listener.Event;
import org.kie.baaas.mcp.app.listener.Listener;
import org.kie.baaas.mcp.app.model.webhook.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.core.instrument.MeterRegistry;

public class WebhookListener implements Listener {

    private static final String UMETER_DAAAS_WEBHOOK_SUCCESS = "daaas.webhook.success";
    private static final String UMETER_DAAAS_WEBHOOK_FAILURE = "daaas.webhook.failure";

    private static final Logger LOG = LoggerFactory.getLogger(WebhookListener.class);

    private final ObjectMapper MAPPER;
    private final HttpClient httpClient;
    private final Webhook webhook;
    private final MeterRegistry meterRegistry;

    final Integer MAX_RETRY;
    final Integer TIMEOUT;

    public WebhookListener(Webhook webhook, ManagedExecutor executorService, MeterRegistry meterRegistry,
            ObjectMapper objectMapper, Integer MAX_RETRY, Integer TIMEOUT) {
        Objects.requireNonNull(MAX_RETRY, "MAX_RETRY cannot be null");
        Objects.requireNonNull(TIMEOUT, "TIMEOUT cannot be null");
        this.webhook = webhook;
        this.meterRegistry = meterRegistry;
        this.MAPPER = objectMapper;
        this.httpClient = HttpClient.newBuilder().executor(executorService).version(HttpClient.Version.HTTP_2).build();
        this.MAX_RETRY = MAX_RETRY;
        this.TIMEOUT = TIMEOUT;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    private CloudEvent toCloudEvent(Event e) throws Exception {
        CloudEvent build = CloudEventBuilder.v1().withId(e.getEventId().orElse(UUID.randomUUID().toString()))
                .withSource(URI.create("daaswebhook")).withType(e.getClass().getCanonicalName())
                .withData(MAPPER.writeValueAsString(e).getBytes()).build();
        return build;
    }

    @Override
    public void onEvent(Event event) {
        LOG.debug("webhook: {} event: {}", webhook.getUrl(), event);
        try {
            CloudEvent ce = toCloudEvent(event);
            meterRegistry.counter("daaas.webhook.invocations", "url", webhook.getUrl().toString()).increment();
            HttpRequest request = HttpRequest.newBuilder().POST(BodyPublishers.ofString(MAPPER.writeValueAsString(ce)))
                    .uri(URI.create(webhook.getUrl().toString())).header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT)).build();
            BodyHandler<String> responseBodyHandler = HttpResponse.BodyHandlers.ofString();
            httpClient.sendAsync(request, responseBodyHandler)
                    .handleAsync((r, t) -> handleAsync(httpClient, request, responseBodyHandler, 1, r, t))
                    .thenCompose(Function.identity());
        } catch (Exception e) {
            LOG.error("Unable to build CloudEvent to notify webhook", e);
        }
    }

    private <T> void handleMetrics(HttpResponse<T> response, Throwable t) {
        if (response != null) {
            if (response.statusCode() != 200) {
                LOG.error("response status {} for webhook {}: {}", response.statusCode(), webhook, response);
                meterRegistry.counter(UMETER_DAAAS_WEBHOOK_FAILURE, "url", webhook.getUrl().toString()).increment();
            } else {
                meterRegistry.counter(UMETER_DAAAS_WEBHOOK_SUCCESS, "url", webhook.getUrl().toString()).increment();
            }
        } else {
            LOG.error("Exception for webhook {}: {}", webhook, t);
            meterRegistry.counter(UMETER_DAAAS_WEBHOOK_FAILURE, "url", webhook.getUrl().toString()).increment();
        }
    }

    private <T> CompletableFuture<HttpResponse<T>> handleAsync(HttpClient client, HttpRequest request, BodyHandler<T> responseBodyHandler, int count, HttpResponse<T> response, Throwable t) {
        handleMetrics(response, t);
        if (count >= MAX_RETRY || (response != null && response.statusCode() == 200)) { // stop after having "insisted" enough retries, or we finally got http 200.
            if (response != null) {
                return CompletableFuture.completedFuture(response);
            } else {
                return CompletableFuture.failedFuture(t);
            }
        }
        return httpClient.sendAsync(request, responseBodyHandler)
                .handleAsync((rr, tt) -> handleAsync(httpClient, request, responseBodyHandler, count + 1, rr, tt))
                .thenCompose(Function.identity());
    }
}
