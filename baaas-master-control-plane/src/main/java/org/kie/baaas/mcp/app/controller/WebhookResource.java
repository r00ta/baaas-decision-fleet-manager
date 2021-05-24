package org.kie.baaas.mcp.app.controller;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.kie.baaas.mcp.api.webhook.WebhookRegistrationRequest;
import org.kie.baaas.mcp.api.webhook.WebhookResponse;
import org.kie.baaas.mcp.app.manager.WebhookManager;
import org.kie.baaas.mcp.app.model.webhook.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/webhooks")
@ApplicationScoped
public class WebhookResource {
    private final Logger LOG = LoggerFactory.getLogger(WebhookResource.class);
    private final WebhookManager webhookManager;

    @Inject
    public WebhookResource(WebhookManager listenerManager) {
        Objects.requireNonNull(listenerManager, "listenerManager cannot be null");
        this.webhookManager = listenerManager;
    }

    @POST
    public Response registerWebhook(WebhookRegistrationRequest webhookReq) {
        LOG.debug("registerWebhook {}", webhookReq);
        Webhook webhook = webhookManager.registerWebhook(webhookReq);
        WebhookResponse result = WebhookResponse.from(webhook.getId(), webhookReq.getUrl());
        return Response.ok().entity(result).build();
    }

    /**
     * support the same semantics as on the /decisions resources where a GET or DELETE works with the <id> parameter set to either the URL or id of the webhook
     */
    @DELETE
    @Path("{lookupRef}")
    public Response unregisterForWebhook(@PathParam("lookupRef") String lookupRef) {
        LOG.debug("unregisterForWebhook {}", lookupRef);
        webhookManager.unregisterForWebhook(lookupRef);
        return Response.ok().build();
    }
}
