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

import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.app.resolvers.CustomerIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central {@link ExceptionMapper} implementation for handling validation failures for requests
 * to the BAaaS API.
 */
@Provider
public class ValidationFailureExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationFailureExceptionMapper.class);

    @Inject
    CustomerIdResolver customerIdResolver;

    @Override
    public Response toResponse(ConstraintViolationException exception) {

        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        String customerId = customerIdResolver.getCustomerId();

        LOGGER.info("Request for customer id '{}' failed validation.", customerId);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(violations.stream().map(violation -> "Field: '" + DecisionRequest.class.getSimpleName() + "." +
                        violation.getPropertyPath() + "' -> Provided value seems not to be valid, explanation: " +
                        violation.getMessage() + violation.getRootBeanClass()))
                .build();
    }
}
