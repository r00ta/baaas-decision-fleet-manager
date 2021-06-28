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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.kie.baaas.mcp.api.DMNJIT;
import org.kie.baaas.mcp.api.DMNJITList;
import org.kie.baaas.mcp.app.dao.DMNJITDAO;
import org.kie.baaas.mcp.app.model.ListResult;

import io.quarkus.security.Authenticated;

import static java.util.Objects.requireNonNull;
import static org.kie.baaas.mcp.app.controller.APIConstants.PAGE;
import static org.kie.baaas.mcp.app.controller.APIConstants.PAGE_DEFAULT;
import static org.kie.baaas.mcp.app.controller.APIConstants.PAGE_MIN;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE_DEFAULT;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE_MAX;
import static org.kie.baaas.mcp.app.controller.APIConstants.SIZE_MIN;

@Path("/decisions/jit")
@ApplicationScoped
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
public class DMNJITResource {

    private DMNJITDAO dmnjitdao;

    @Inject
    public DMNJITResource(DMNJITDAO dmnjitdao) {
        requireNonNull(dmnjitdao, "dmnjitdao cannot be null");
        this.dmnjitdao = dmnjitdao;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public Response listDmnJits(@DefaultValue(PAGE_DEFAULT) @Min(PAGE_MIN) @QueryParam(PAGE) int page, @DefaultValue(SIZE_DEFAULT) @Min(SIZE_MIN) @Max(SIZE_MAX) @QueryParam(SIZE) int size) {
        ListResult<DMNJIT> dmnjitListResult = dmnjitdao.listAll(page, size);
        DMNJITList dmnjitList = new DMNJITList();
        dmnjitList.setItems(dmnjitListResult.getItems());
        dmnjitList.setPage(dmnjitListResult.getPage());
        dmnjitList.setSize(dmnjitListResult.getSize());
        dmnjitList.setTotal(dmnjitListResult.getTotal());
        return Response.ok(dmnjitList).build();
    }
}
