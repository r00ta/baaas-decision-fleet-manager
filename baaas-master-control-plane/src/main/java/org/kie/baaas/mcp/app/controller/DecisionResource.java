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
import org.kie.baaas.mcp.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.mcp.app.manager.DecisionManager;
import org.kie.baaas.mcp.app.model.DecisionVersion;
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

    private final DecisionManager decisionManager;

    private final DecisionMapper decisionMapper;

    @Inject
    public DecisionResource(CustomerIdResolver customerIdResolver, DecisionManager decisionManager, DecisionMapper decisionMapper) {

        requireNonNull(customerIdResolver, "customerIdResolver cannot be null");
        requireNonNull(decisionManager, "decisionManager cannot be null");
        requireNonNull(decisionMapper, "decisionMapper cannot be null");

        this.customerIdResolver = customerIdResolver;
        this.decisionManager = decisionManager;
        this.decisionMapper = decisionMapper;
    }

    @PUT
    @Path("{id}/versions/{version}")
    public Response rollbackToDecisionVersion(String id, long version) {
        //TODO - invoke the DecisionManager to rollback to the specified version
        //TODO - marshall the response back to our DTOs
        return null;
    }

    @DELETE
    @Path("{id}")
    public Response deleteDecision(String id) {
        //TODO - invoke DecisionManager to delete the Decision
        //TODO - marshall the response back into our DTOs
        return null;
    }

    @DELETE
    @Path("{id}/versions/{version}")
    public Response deleteDecisionVersion(@PathParam("id") String id, @PathParam("version") long version) {
        //TODO - invoke DecisionManager to delete decision version
        //TODO - Marshall into our DTOs
        return null;
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
        // TODO - invoke the DecisionManager to find details of the specific Decision Version
        // TODO - marshall response back into our DTOs
        return null;
    }

    @GET
    @Path("{id}")
    public Response getDecision(@PathParam("id") String id) {

        //TODO - invoke the DecisionManager to find details of the specific Decision
        //TODO - marshall response from DecisionManager into our DTOs
        return null;
    }

    @GET
    @Path("{id}/versions")
    public Response listDecisionVersions(@PathParam("id") String id) {
        // TODO - invoke the DecisionManager to get a list of all versions for this Decision
        // TODO - marshall returned list from DecisionManager into our DTOs
        // TODO - should id be either the internal DB id _or_ the decision name?
        return null;
    }

    @GET
    public Response listDecisions() {
        //TODO - invoke the DecisionManager to get a list of decisions
        //TODO - marshall the returned list into our DTOs
        return null;
    }

    @POST
    public Response createOrUpdateDecision(@Valid DecisionRequest decisionsRequest) {

        String customerId = customerIdResolver.getCustomerId();
        LOGGER.info("Decision with name '{}' received for processing for customer id '{}'...", decisionsRequest.getName(), customerId);

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerId, decisionsRequest);
        DecisionResponse decisionResponse = decisionMapper.mapVersionToDecisionResponse(decisionVersion);
        return Response.status(Response.Status.CREATED).entity(decisionResponse).build();
    }
}