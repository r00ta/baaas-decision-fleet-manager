/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.kie.baaas.mcp.app.controller;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.kie.baaas.mcp.api.webhook.WebhookRegistrationRequest;
import org.kie.baaas.mcp.api.webhook.WebhookResponse;
import org.kie.baaas.mcp.api.webhook.WebhookResponseList;
import org.kie.baaas.mcp.app.manager.WebhookManager;
import org.kie.baaas.mcp.app.model.ListResult;
import org.kie.baaas.mcp.app.model.webhook.Webhook;
import org.kie.baaas.mcp.app.resolvers.CustomerIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

import static org.kie.baaas.mcp.app.controller.APIConstants.PAGE;
import static org.kie.baaas.mcp.app.controller.APIConstants.PAGE_DEFAULT;
import static org.kie.baaas.mcp.app.controller.APIConstants.PAGE_MIN;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE_DEFAULT;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE_MAX;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE_MIN;

@Path("/webhooks")
@ApplicationScoped
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
public class WebhookResource {

    private final Logger LOG = LoggerFactory.getLogger(WebhookResource.class);

    private final WebhookManager webhookManager;

    @Inject
    CustomerIdResolver customerIdResolver;

    @Inject
    SecurityIdentity identity;

    @Inject
    public WebhookResource(WebhookManager listenerManager) {
        Objects.requireNonNull(listenerManager, "listenerManager cannot be null");
        this.webhookManager = listenerManager;
    }

    @POST
    @Authenticated
    public Response registerWebhook(WebhookRegistrationRequest webhookReq) {
        LOG.debug("registerWebhook {}", webhookReq);

        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        Webhook webhook = webhookManager.registerWebhook(customerId, webhookReq);
        WebhookResponse result = WebhookResponse.from(webhook.getId(), webhookReq.getUrl());
        return Response.ok().entity(result).build();
    }

    /**
     * support the same semantics as on the /decisions resources where a GET or DELETE works with the <id> parameter set to either the URL or id of the webhook
     */
    @DELETE
    @Path("{lookupRef}")
    @Authenticated
    public Response unregisterForWebhook(@PathParam("lookupRef") String lookupRef) {
        LOG.debug("unregisterForWebhook {}", lookupRef);

        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        webhookManager.unregisterForWebhook(customerId, lookupRef);
        return Response.ok().build();
    }

    @GET
    @Authenticated
    public Response getWebooks(@QueryParam(PAGE) @Min(PAGE_MIN) @DefaultValue(PAGE_DEFAULT) int page, @QueryParam(SIZE) @DefaultValue(SIZE_DEFAULT) @Min(SIZE_MIN) @Max(SIZE_MAX) int size) {
        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        ListResult<Webhook> listResult = webhookManager.listCustomerWebhooks(customerId, page, size);
        List<WebhookResponse> webhooks = listResult.getItems().stream().map(e -> WebhookResponse.from(e.getId(), e.getUrl())).collect(Collectors.toList());
        WebhookResponseList result = new WebhookResponseList();
        result.setItems(webhooks);
        result.setPage(listResult.getPage());
        result.setSize(listResult.getSize());
        result.setTotal(listResult.getTotal());
        return Response.ok().entity(result).build();
    }
}
