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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.kie.baaas.mcp.api.DMNJIT;
import org.kie.baaas.mcp.api.DMNJITList;
import org.kie.baaas.mcp.app.dao.DMNJITDAO;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@Path("/decisions/jit")
@ApplicationScoped
public class DMNJITResource {

    private DMNJITDAO dmnjitdao;

    @Inject
    public DMNJITResource(DMNJITDAO dmnjitdao) {
        requireNonNull(dmnjitdao, "dmnjitdao cannot be null");
        this.dmnjitdao = dmnjitdao;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listDmnJits() {

        DMNJIT dmnjit = dmnjitdao.findOne();
        DMNJITList dmnjitList = new DMNJITList();
        dmnjitList.setItems(singletonList(dmnjit));
        return Response.ok(dmnjitList).build();
    }
}
