package org.kie.baaas.mcp.app.controller;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.kie.baaas.mcp.api.webhook.Webhook;
import org.kie.baaas.mcp.api.webhook.WebhookRegistrationRequest;
import org.kie.baaas.mcp.app.listener.ListenerManager;
import org.kie.baaas.mcp.app.webhook.WebhookListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/webhooks")
@ApplicationScoped
public class WebhookResource {
    private final Logger LOG = LoggerFactory.getLogger(WebhookResource.class);
    private final ListenerManager listenerManager;
    private final ManagedExecutor executorService;

    @Inject
    public WebhookResource(ListenerManager listenerManager, ManagedExecutor executorService) {
        Objects.requireNonNull(listenerManager, "listenerManager cannot be null");
        Objects.requireNonNull(executorService, "executorService cannot be null");

        this.listenerManager = listenerManager;
        this.executorService = executorService;
    }

    @POST
    public Response registerWebhook(WebhookRegistrationRequest webhookReq) {
        LOG.debug("registerWebhook {}", webhookReq);
        String id = UUID.randomUUID().toString();
        Webhook webhook = Webhook.from(id, webhookReq.getUrl());
        listenerManager.addListener(new WebhookListener(webhook, executorService));
        return Response.ok().entity(webhook).build();
    }

    /**
     * support the same semantics as on the /decisions resources where a GET or DELETE works with the <id> parameter set to either the URL or id of the webhook
     */
    @DELETE
    @Path("{lookupRef}")
    public Response unregisterForWebhook(@PathParam("lookupRef") String lookupRef) {
        LOG.debug("unregisterForWebhook {}", lookupRef);
        final Predicate<WebhookListener> lookup = (WebhookListener l) -> l.getWebhook().getUrl().toString().equals(lookupRef) || l.getWebhook().getId().equals(lookupRef);
        List<WebhookListener> toBeRemoved = listenerManager.getListeners()
                .stream()
                .filter(WebhookListener.class::isInstance)
                .map(WebhookListener.class::cast)
                .filter(lookup::test)
                .collect(Collectors.toList());
        toBeRemoved.forEach(listenerManager::removeListener);
        return Response.ok().build();
    }
}
