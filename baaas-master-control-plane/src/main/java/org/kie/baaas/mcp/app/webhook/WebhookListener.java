package org.kie.baaas.mcp.app.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.kie.baaas.mcp.api.webhook.Webhook;
import org.kie.baaas.mcp.app.listener.Event;
import org.kie.baaas.mcp.app.listener.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;

public class WebhookListener implements Listener {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookListener.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(JsonFormat.getCloudEventJacksonModule());

    private final HttpClient httpClient;
    private final Webhook webhook;

    public WebhookListener(Webhook webhook, ManagedExecutor executorService) {
        this.webhook = webhook;
        this.httpClient = HttpClient.newBuilder()
                .executor(executorService)
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public Webhook getWebhook() {
        return webhook;
    }

    private static CloudEvent toCloudEvent(Event e) throws Exception {
        CloudEvent build = CloudEventBuilder.v1()
                .withId(e.getEventId().orElse(UUID.randomUUID().toString()))
                .withSource(URI.create("daaswebhook"))
                .withType(e.getClass().getCanonicalName())
                .withData(new ObjectMapper().writeValueAsString(e).getBytes())
                .build();
        return build;
    }

    @Override
    public void onEvent(Event event) {
        LOG.debug("webhook: {} event: {}", webhook.getUrl(), event);
        try {
            CloudEvent ce = toCloudEvent(event);
            httpClient.sendAsync(
                    HttpRequest.newBuilder()
                            .POST(BodyPublishers.ofString(MAPPER.writeValueAsString(ce)))
                            .uri(URI.create(webhook.getUrl().toString()))
                            .header("Accept", "application/json")
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOG.error("Unable to build CloudEvent to notify webhook", e);
        }
    }
}
