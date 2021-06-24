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

package org.kie.baaas.mcp.app.manager;

import javax.ws.rs.core.Response;

import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;

/**
 * Indicates that we are trying to perform an action on a Decision Version that does not exist.
 */
public class NoSuchDecisionVersionException extends MasterControlPlaneException {

    public NoSuchDecisionVersionException(String message) {
        super(message);
    }

    public NoSuchDecisionVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.NOT_FOUND.getStatusCode();
    }
}
