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

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kie.baaas.mcp.api.decisions.DecisionsRequest;
import org.kie.baaas.mcp.api.decisions.DecisionsResponse;
import org.kie.baaas.mcp.app.manager.DecisionManager;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.resolvers.CustomerIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/decisions")
@ApplicationScoped
public class DecisionsResource {

    private final Logger LOGGER = LoggerFactory.getLogger(DecisionsResource.class);

    @Inject
    Validator validator;

    @Inject
    private CustomerIdResolver customerIdResolver;

    @Inject
    private DecisionManager decisionManager;

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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response process(DecisionsRequest decisionsRequest) {

        Set<ConstraintViolation<DecisionsRequest>> violations = validator.validate(decisionsRequest);

        if (violations.isEmpty()) {

            LOGGER.info("Decision {} received for processing...", decisionsRequest.getName());
            DecisionsResponse decisionsResponse = copyFields(decisionsRequest);

            // TODO persist on S3 while saving decision on database
            decisionsResponse.getResponseModel().setMd5("test");
            Decision d = decisionManager.createOrUpdateVersion("customer-1", decisionsResponse);

            decisionsResponse.setId(d.getId());
            decisionsResponse.setSubmittedAt(d.getCurrentVersion().getSubmittedAt());
            decisionsResponse.setVersion(d.getCurrentVersion().getVersion());

            // TODO deploy on ccp

            return Response.status(Response.Status.CREATED).entity(decisionsResponse).build();
        } else {

            LOGGER.info("Decision {} received for processing is not valid, check response.", decisionsRequest.getName());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(violations.stream().map(violation -> "Field: 'decisionsRequest." +
                            violation.getPropertyPath() + "' -> Provided value seems not to be valid, explanation: " +
                            violation.getMessage())).build();
        }
    }

    /**
     * Copy common fields from @{link DecisionsRequest} to {@link DecisionsResponse}
     *
     * @param @{link DecisionsRequest} decisionsRequest
     * @return @link DecisionsResponse} decisionsResponse
     */
    private DecisionsResponse copyFields(DecisionsRequest decisionsRequest) {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            DecisionsResponse copy = mapper.readValue(mapper.writeValueAsString(decisionsRequest), DecisionsResponse.class);
            return copy;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new DecisionsResponse();
        }
    }
}


