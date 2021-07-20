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

package org.kie.baaas.dfm.app.controller;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.kie.baaas.dfm.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.dfm.app.manager.DecisionLifecycle;
import org.kie.baaas.dfm.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.dfm.app.model.Decision;
import org.kie.baaas.dfm.app.model.DecisionVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/admin")
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
public class AdminResource {

    private final Logger LOGGER = LoggerFactory.getLogger(AdminResource.class);

    private final DecisionLifecycle decisionLifecycle;

    private final DecisionMapper decisionMapper;

    @Inject
    public AdminResource(DecisionLifecycleOrchestrator decisionLifecycle,
            DecisionMapper decisionMapper) {
        requireNonNull(decisionLifecycle, "decisionLifecycle cannot be null");
        requireNonNull(decisionMapper, "decisionMapper cannot be null");

        this.decisionLifecycle = decisionLifecycle;
        this.decisionMapper = decisionMapper;
    }

    @GET
    @Path("/decisions")
    @RolesAllowed({ "admin" })
    public Response getDecisions() {
        List<Decision> decisions = decisionLifecycle.listDecisions();
        return Response.ok(decisions.stream()
                .map(decisionMapper::mapToDecisionResponse)
                .collect(toList())).build();
    }

    @DELETE
    @Path("/decisions/{id}")
    @RolesAllowed({ "admin" })
    public Response deleteDecision(@PathParam("id") String id) {
        LOGGER.info("Deleting decision with id or name '{}'...", id);
        decisionLifecycle.deleteDecision(id);
        return Response.ok().build();
    }

    @DELETE
    @Path("/decisions/{id}/versions/{version}")
    @RolesAllowed({ "admin" })
    public Response deleteDecisionVersion(@PathParam("id") String id, @PathParam("version") long version) {
        LOGGER.info("Deleting version '{}' of Decision with id or name '{}'...", version, id);

        DecisionVersion decisionVersion = decisionLifecycle.deleteVersion(id, version);
        return Response.ok(decisionMapper.mapVersionToDecisionResponse(decisionVersion)).build();
    }
}
