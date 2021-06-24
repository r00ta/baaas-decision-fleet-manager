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

package org.kie.baaas.mcp.app.controller.exceptionmappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized {@link ExceptionMapper} for the {@link MasterControlPlaneException} hierarchy.
 */
@Provider
public class MasterControlPlaneExceptionMapper implements ExceptionMapper<MasterControlPlaneException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MasterControlPlaneExceptionMapper.class);

    //TODO - Extend this with support for Error codes and useful payload
    @Override
    public Response toResponse(MasterControlPlaneException e) {
        LOGGER.error("Failure", e);
        return Response.status(e.getStatusCode()).entity(e.getMessage()).build();
    }
}
