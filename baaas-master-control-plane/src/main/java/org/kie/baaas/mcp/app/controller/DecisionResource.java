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

import java.io.ByteArrayOutputStream;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.api.decisions.DecisionResponse;
import org.kie.baaas.mcp.api.decisions.DecisionResponseList;
import org.kie.baaas.mcp.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.mcp.app.manager.DecisionLifecycle;
import org.kie.baaas.mcp.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.DecisionVersionStatus;
import org.kie.baaas.mcp.app.model.ListResult;
import org.kie.baaas.mcp.app.resolvers.CustomerIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

import static java.util.Objects.requireNonNull;
import static org.kie.baaas.mcp.app.controller.APIConstants.PAGE;
import static org.kie.baaas.mcp.app.controller.APIConstants.PAGE_DEFAULT;
import static org.kie.baaas.mcp.app.controller.APIConstants.PAGE_MIN;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE_DEFAULT;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE_MAX;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE_MIN;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/decisions")
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
public class DecisionResource {

    private final Logger LOGGER = LoggerFactory.getLogger(DecisionResource.class);

    private final DecisionLifecycle decisionLifecycle;

    private final DecisionMapper decisionMapper;

    @Inject
    CustomerIdResolver customerIdResolver;

    @Inject
    SecurityIdentity identity;

    @Inject
    public DecisionResource(DecisionLifecycleOrchestrator decisionLifecycle,
            DecisionMapper decisionMapper) {
        requireNonNull(decisionLifecycle, "decisionLifecycle cannot be null");
        requireNonNull(decisionMapper, "decisionMapper cannot be null");

        this.decisionLifecycle = decisionLifecycle;
        this.decisionMapper = decisionMapper;
    }

    private Response mapDecisionVersion(DecisionVersion decisionVersion) {
        DecisionResponse response = decisionMapper.mapVersionToDecisionResponse(decisionVersion);
        return Response.ok(response).build();
    }

    @PUT
    @Path("{id}/versions/{version}")
    @Authenticated
    public Response setCurrentVersion(@PathParam("id") String id, @PathParam("version") long version) {
        return Response.status(400).entity("This endpoint/feature has been temporary disabled. See https://issues.redhat.com/browse/BAAAS-156").build();
        //        String customerId = customerIdResolver.getCustomerId();
        //        LOGGER.info("Setting new current version '{}' of Decision with id or name '{}' for customer '{}'...", id, version, customerId);
        //        DecisionVersion decisionVersion = decisionLifecycle.setCurrentVersion(customerId, id, version);
        //        return mapDecisionVersion(decisionVersion);
    }

    @DELETE
    @Path("{id}")
    @Authenticated
    public Response deleteDecision(@PathParam("id") String id) {
        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        LOGGER.info("Deleting decision with id or name '{}' for customer id '{}'...", id, customerId);
        decisionLifecycle.deleteDecision(customerId, id);
        return Response.ok().build();
    }

    @DELETE
    @Path("{id}/versions/{version}")
    @Authenticated
    public Response deleteDecisionVersion(@PathParam("id") String id, @PathParam("version") long version) {
        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        LOGGER.info("Deleting version '{}' of Decision with id or name '{}' for customer '{}'...",
                version, id, customerId);

        DecisionVersion decisionVersion = decisionLifecycle.deleteVersion(customerId, id, version);
        return mapDecisionVersion(decisionVersion);
    }

    @GET
    @Path("{id}/versions/{version}/dmn")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Authenticated
    public Response getDecisionVersionDMN(@PathParam("id") String id, @PathParam("version") long version) {
        // TODO returns a formatted xml file.

        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        LOGGER.info("Requesting Decision version '{}' of with id or name '{}' for customer '{}' to be downloaded...",
                version, id, customerId);

        ByteArrayOutputStream byteArrayOutputStream = decisionLifecycle.getDMN(customerId, id, version);

        Response.ResponseBuilder response = Response.ok((StreamingOutput) output -> byteArrayOutputStream.writeTo(output));
        response.header("Content-Disposition", "attachment;filename=" + id + ".xml");
        response.header("Content-Type", MediaType.APPLICATION_XML);

        return response.build();
    }

    @GET
    @Path("{id}/building")
    @Authenticated
    public Response getBuildingVersion(@PathParam("id") String decisionIdOrName) {
        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        LOGGER.info("Getting details of BUILDING version of Decision with id or name '{}' for customer '{}'...", decisionIdOrName, customerId);

        DecisionVersion decisionVersion = decisionLifecycle.getBuildingVersion(customerId, decisionIdOrName);
        return mapDecisionVersion(decisionVersion);
    }

    @GET
    @Path("{id}/versions/{version}")
    @Authenticated
    public Response getDecisionVersion(@PathParam("id") String id, @PathParam("version") long version) {
        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        LOGGER.info("Getting details of Decision Version '{}' for Decision with id or name '{}' for customer '{}'...", id, version, customerId);
        DecisionVersion decisionVersion = decisionLifecycle.getVersion(customerId, id, version);
        return mapDecisionVersion(decisionVersion);
    }

    @GET
    @Path("{id}")
    @Authenticated
    public Response getDecision(@PathParam("id") String id) {
        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        LOGGER.info("Getting details of '{}' version of Decision with id or name '{}' for customer '{}'...", DecisionVersionStatus.CURRENT, id, customerId);
        DecisionVersion decisionVersion = decisionLifecycle.getCurrentVersion(customerId, id);
        return mapDecisionVersion(decisionVersion);
    }

    @GET
    @Path("{id}/versions")
    @Authenticated
    public Response listDecisionVersions(@PathParam("id") String id, @DefaultValue(PAGE_DEFAULT) @Min(PAGE_MIN) @QueryParam(PAGE) int page,
            @DefaultValue(SIZE_DEFAULT) @Min(SIZE_MIN) @Max(SIZE_MAX) @QueryParam(SIZE) int size) {
        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        LOGGER.info("Listing all versions for Decision with id or name '{}' for customer '{}'...", id, customerId);
        ListResult<DecisionVersion> versions = decisionLifecycle.listDecisionVersions(customerId, id, page, size);
        DecisionResponseList responseList = decisionMapper.mapVersionsToDecisionResponseList(versions);
        return Response.ok(responseList).build();
    }

    @GET
    @Authenticated
    public Response listDecisions(@DefaultValue(PAGE_DEFAULT) @Min(PAGE_MIN) @QueryParam(PAGE) int page, @DefaultValue(SIZE_DEFAULT) @Min(SIZE_MIN) @Max(SIZE_MAX) @QueryParam(SIZE) int size) {
        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        LOGGER.info("Listing all Decisions for customer with id '{}...'", customerId);
        ListResult<Decision> decisions = decisionLifecycle.listDecisions(customerId, page, size);
        DecisionResponseList responseList = decisionMapper.mapToDecisionResponseList(decisions);
        return Response.ok(responseList).build();
    }

    @POST
    @Authenticated
    public Response createOrUpdateDecision(@Valid DecisionRequest decisionsRequest) {
        String customerId = customerIdResolver.getCustomerId(identity.getPrincipal());
        LOGGER.info("Decision with name '{}' received for processing for customer id '{}'...", decisionsRequest.getName(), customerId);
        DecisionVersion decisionVersion = decisionLifecycle.createOrUpdateVersion(customerId, decisionsRequest);
        DecisionResponse decisionResponse = decisionMapper.mapVersionToDecisionResponse(decisionVersion);
        return Response.status(Response.Status.CREATED).entity(decisionResponse).build();
    }

}
