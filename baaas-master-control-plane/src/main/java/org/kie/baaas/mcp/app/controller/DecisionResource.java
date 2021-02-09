/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.baaas.mcp.app.controller;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.api.decisions.DecisionResponse;
import org.kie.baaas.mcp.api.decisions.DecisionResponseList;
import org.kie.baaas.mcp.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.mcp.app.manager.DecisionLifecycle;
import org.kie.baaas.mcp.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.DecisionVersionStatus;
import org.kie.baaas.mcp.app.resolvers.CustomerIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/decisions")
@ApplicationScoped
public class DecisionResource {

    private final Logger LOGGER = LoggerFactory.getLogger(DecisionResource.class);

    private final CustomerIdResolver customerIdResolver;

    private final DecisionLifecycle decisionLifecycle;

    private final DecisionMapper decisionMapper;

    @Inject
    public DecisionResource(CustomerIdResolver customerIdResolver, DecisionLifecycleOrchestrator decisionLifecycle, DecisionMapper decisionMapper) {

        requireNonNull(customerIdResolver, "customerIdResolver cannot be null");
        requireNonNull(decisionLifecycle, "decisionLifecycle cannot be null");
        requireNonNull(decisionMapper, "decisionMapper cannot be null");

        this.customerIdResolver = customerIdResolver;
        this.decisionLifecycle = decisionLifecycle;
        this.decisionMapper = decisionMapper;
    }

    private Response mapDecisionVersion(DecisionVersion decisionVersion) {
        DecisionResponse response = decisionMapper.mapVersionToDecisionResponse(decisionVersion);
        return Response.ok(response).build();
    }

    @PUT
    @Path("{id}/versions/{version}")
    public Response rollbackToDecisionVersion(String id, long version) {

        String customerId = customerIdResolver.getCustomerId();
        LOGGER.info("Rolling back to version '{}' of Decision with id or name '{}' for customer '{}'...", id, version, customerId);
        DecisionVersion decisionVersion = decisionLifecycle.rollbackToVersion(customerId, id, version);
        return mapDecisionVersion(decisionVersion);
    }

    @DELETE
    @Path("{id}")
    public Response deleteDecision(String id) {

        String customerId = customerIdResolver.getCustomerId();
        LOGGER.info("Deleting decision with id or name '{}' for customer id '{}'...", id, customerId);
        Decision decision = decisionLifecycle.deleteDecision(customerId, id);
        DecisionResponse response = decisionMapper.mapToDecisionResponse(decision);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("{id}/versions/{version}")
    public Response deleteDecisionVersion(@PathParam("id") String id, @PathParam("version") long version) {

        String customerId = customerIdResolver.getCustomerId();
        LOGGER.info("Deleting version '{}' of Decision with id or name '{}' for customer '{}'...");

        DecisionVersion decisionVersion = decisionLifecycle.deleteVersion(customerId, id, version);
        return mapDecisionVersion(decisionVersion);
    }

    @GET
    @Path("{id}/versions/{version}/dmn")
    public Response getDecisionVersionDMN(@PathParam("id") String id, @PathParam("version") long version) {
        //TODO - invoke the DecisionManager to retrieve the DMN for this specific version
        //TODO - marshall into our DTOs
        return null;
    }

    @GET
    @Path("{id}/versions/{version}")
    public Response getDecisionVersion(@PathParam("id") String id, @PathParam("version") long version) {

        String customerId = customerIdResolver.getCustomerId();
        LOGGER.info("Getting details of Decision Version '{}' for Decision with id or name '{}' for customer '{}'...", id, version, customerId);
        DecisionVersion decisionVersion = decisionLifecycle.getVersion(customerId, id, version);
        return mapDecisionVersion(decisionVersion);
    }

    @GET
    @Path("{id}")
    public Response getDecision(@PathParam("id") String id) {

        String customerId = customerIdResolver.getCustomerId();
        LOGGER.info("Getting details of '{}' version of Decision with id or name '{}' for customer '{}'...", DecisionVersionStatus.CURRENT, id, customerId);
        DecisionVersion decisionVersion = decisionLifecycle.getCurrentVersion(customerIdResolver.getCustomerId(), id);
        return mapDecisionVersion(decisionVersion);
    }

    @GET
    @Path("{id}/versions")
    public Response listDecisionVersions(@PathParam("id") String id) {

        String customerId = customerIdResolver.getCustomerId();
        LOGGER.info("Listing all versions for Decision with id or name '{}' for customer '{}'...", id, customerId);
        List<DecisionVersion> versions = decisionLifecycle.listDecisionVersions(customerId, id);
        DecisionResponseList responseList = decisionMapper.mapVersionsToDecisionResponseList(versions);
        return Response.ok(responseList).build();
    }

    @GET
    public Response listDecisions() {

        String customerId = customerIdResolver.getCustomerId();
        LOGGER.info("Listing all Decisions for customer with id '{}...'", customerId);
        List<Decision> decisions = decisionLifecycle.listDecisions(customerId);
        DecisionResponseList responseList = decisionMapper.mapToDecisionResponseList(decisions);
        return Response.ok(responseList).build();
    }

    @POST
    public Response createOrUpdateDecision(@Valid DecisionRequest decisionsRequest) {

        String customerId = customerIdResolver.getCustomerId();
        LOGGER.info("Decision with name '{}' received for processing for customer id '{}'...", decisionsRequest.getName(), customerId);

        DecisionVersion decisionVersion = decisionLifecycle.createOrUpdateVersion(customerId, decisionsRequest);
        DecisionResponse decisionResponse = decisionMapper.mapVersionToDecisionResponse(decisionVersion);
        return Response.status(Response.Status.CREATED).entity(decisionResponse).build();
    }
}