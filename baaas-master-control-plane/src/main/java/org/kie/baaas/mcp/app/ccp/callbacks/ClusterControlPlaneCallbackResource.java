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

package org.kie.baaas.mcp.app.ccp.callbacks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.kie.baaas.dfs.api.Phase;
import org.kie.baaas.dfs.api.Webhook;
import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;
import org.kie.baaas.mcp.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.mcp.app.model.deployment.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives callbacks from the ClusterControlPlane to update progress on deployment
 * of a Decision Service.
 */
@Path("/callback/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ClusterControlPlaneCallbackResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterControlPlaneCallbackResource.class);

    private final DecisionLifecycleOrchestrator decisionLifecycleOrch;

    @Inject
    public ClusterControlPlaneCallbackResource(DecisionLifecycleOrchestrator decisionLifecycleOrch) {
        this.decisionLifecycleOrch = decisionLifecycleOrch;
    }

    private Deployment createDeployment(Webhook webhook) {
        Deployment deployment = new Deployment();
        deployment.setName(webhook.getDecision());
        deployment.setNamespace(webhook.getNamespace());
        deployment.setVersionName(webhook.getVersionResource());
        deployment.setStatusMessage(webhook.getMessage());

        /* Failed deployments have no endpoint */
        if (webhook.getEndpoint() != null) {
            deployment.setUrl(webhook.getEndpoint().toString());
        }
        return deployment;
    }

    @POST
    @Path("decisions/{id}/versions/{version}")
    public Response processClusterControlPlaneCallback(Webhook webhook, @PathParam("id") String decisionIdOrName, @PathParam("version") long version) {

        LOGGER.info("Received callback for decision with idOrName '{}' at version '{}' for customer '{}'. Phase: '{}'", decisionIdOrName, version, webhook.getCustomer(), webhook.getPhase());

        Deployment deployment = createDeployment(webhook);
        if (Phase.CURRENT.equals(webhook.getPhase())) {
            decisionLifecycleOrch.deployed(webhook.getCustomer(), decisionIdOrName, version, deployment);
        } else if (Phase.FAILED.equals(webhook.getPhase())) {
            decisionLifecycleOrch.failed(webhook.getCustomer(), decisionIdOrName, version, deployment);
        } else {
            throw new MasterControlPlaneException("Received unsupported phase '" + webhook.getPhase() + "' in callback.");
        }

        return Response.ok().build();
    }
}