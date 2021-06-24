package org.kie.baaas.mcp.app.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.UUID;

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

    private static final Logger LOG = LoggerFactory.getLogger(WebhookListener.class);

    private final ObjectMapper MAPPER;
    private final HttpClient httpClient;
    private final Webhook webhook;
    private final MeterRegistry meterRegistry;

    public WebhookListener(Webhook webhook, ManagedExecutor executorService, MeterRegistry meterRegistry, ObjectMapper objectMapper) {
        this.webhook = webhook;
        this.meterRegistry = meterRegistry;
        this.MAPPER = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .executor(executorService)
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public Webhook getWebhook() {
        return webhook;
    }

    private CloudEvent toCloudEvent(Event e) throws Exception {
        CloudEvent build = CloudEventBuilder.v1()
                .withId(e.getEventId().orElse(UUID.randomUUID().toString()))
                .withSource(URI.create("daaswebhook"))
                .withType(e.getClass().getCanonicalName())
                .withData(MAPPER.writeValueAsString(e).getBytes())
                .build();
        return build;
    }

    @Override
    public void onEvent(Event event) {
        LOG.debug("webhook: {} event: {}", webhook.getUrl(), event);
        try {
            CloudEvent ce = toCloudEvent(event);
            meterRegistry.counter("daaas.webhook.invocations", "url", webhook.getUrl().toString()).increment();
            httpClient.sendAsync(
                    HttpRequest.newBuilder()
                            .POST(BodyPublishers.ofString(MAPPER.writeValueAsString(ce)))
                            .uri(URI.create(webhook.getUrl().toString()))
                            .header("Accept", "application/json")
                            .build(),
                    HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(r -> {
                        int status = r.statusCode();
                        if (status != 200) {
                            LOG.error("response status {} for webhook {}: {}", status, webhook, r);
                            meterRegistry.counter("daaas.webhook.failure", "url", webhook.getUrl().toString()).increment();
                        } else {
                            meterRegistry.counter("daaas.webhook.success", "url", webhook.getUrl().toString()).increment();
                        }
                        return r;
                    });
        } catch (Exception e) {
            LOG.error("Unable to build CloudEvent to notify webhook", e);
        }
    }
}
