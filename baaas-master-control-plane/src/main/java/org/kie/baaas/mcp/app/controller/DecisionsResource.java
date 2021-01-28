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

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kie.baaas.mcp.api.Decisions;
import org.kie.baaas.mcp.api.DecisionsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/decisions")
@ApplicationScoped
public class DecisionsResource {

    private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    Validator validator;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response process(Decisions decisions) {

        DecisionsResponse decisionsResponse = copyFields(decisions);
        decisionsResponse.setSubmittedAt(LocalDateTime.now().toString());

        Set<ConstraintViolation<Decisions>> violations = validator.validate(decisions);

        if (violations.isEmpty()) {

            LOGGER.info("Decision {} received for processing...", decisions.getName());
            // further process

            return Response.ok().build();
        } else {

            decisionsResponse.setSubmittedAt(LocalDateTime.now().toString());

            LOGGER.info("Decision {} received for processing is not valid, check response.", decisionsResponse.getName());

            decisionsResponse.setViolations(violations.stream()
                                                    .map(violation -> "Field: 'Decisions." + violation.getPropertyPath() + "' -> Provided value seems not to be valid, explanation: " + violation.getMessage())
                                                    .collect(Collectors.joining("| ")));

            return Response.status(Response.Status.BAD_REQUEST).entity(decisionsResponse).build();
        }
    }

    /**
     * Copy common fields from @{link Decisions} to {@link DecisionsResponse}
     *
     * @param @{link Decisions} decisions
     * @return @link DecisionsResponse} decisionsResponse
     */
    private DecisionsResponse copyFields(Decisions decisions) {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            DecisionsResponse copy = mapper.readValue(mapper.writeValueAsString(decisions), DecisionsResponse.class);
            return copy;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new DecisionsResponse();
        }
    }
}


