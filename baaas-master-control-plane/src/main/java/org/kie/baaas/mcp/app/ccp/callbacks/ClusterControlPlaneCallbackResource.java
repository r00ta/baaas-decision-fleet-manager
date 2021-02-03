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

package org.kie.baaas.mcp.app.ccp.callbacks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.kie.baaas.mcp.app.manager.DecisionManager;

@ApplicationScoped
public class ClusterControlPlaneCallbackResource {

    private final DecisionManager decisionManager;

    @Inject
    public ClusterControlPlaneCallbackResource(DecisionManager decisionManager) {
        this.decisionManager = decisionManager;
    }

    @Path("/callback")
    @Produces(MediaType.APPLICATION_JSON)
    public Response processClusterControlPlaneCallback() {
        //TODO
        // - What is the JSON payload call back from the CCP (discuss and confirm with Ruben)
        // - This will most likely require an update to the current deployed and failed callback methods on DecisionManager
        // - There may be additional callbacks that we need to support please see: https://docs.google.com/document/d/1DvcwJUKSzzdiEhYbiID66aUnHex793HbFQmdWyp1tHs/edit#
        // - Determine if the deployment was success/failure or something else
        // - Invoke the correct callback on the manager.

        if (didDeploymentSucceed()) {
            //decisionManager.deployed();
        } else {
            // decisionManager.failed();
        }

        return null;
    }

    private boolean didDeploymentSucceed() {
        //TODO
        return false;
    }
}
