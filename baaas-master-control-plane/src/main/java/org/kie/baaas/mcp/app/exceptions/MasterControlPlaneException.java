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

package org.kie.baaas.mcp.app.exceptions;

/**
 * Root of the Exception Hierarchy for the MasterControlPlane.
 * <p>
 * Sub-class this Exception to provide situation-specific Exceptions that provide meaningful context
 * to the user.
 */
public class MasterControlPlaneException extends RuntimeException {

    public MasterControlPlaneException(String message) {
        super(message);
    }

    public MasterControlPlaneException(String message, Throwable cause) {
        super(message, cause);
    }
}
