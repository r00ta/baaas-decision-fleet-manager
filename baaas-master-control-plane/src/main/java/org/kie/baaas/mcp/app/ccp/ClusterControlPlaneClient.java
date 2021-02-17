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

package org.kie.baaas.mcp.app.ccp;

import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;

/**
 * Interface for sending requests to the Cluster Control Plane
 */
public interface ClusterControlPlaneClient {

    ClusterControlPlane getClusterControlPlane();

    /**
     * Request deployment of the specific DecisionVersion. This encompasses a new decision,
     * and update to an existing Decision or a rollback to a previous version of a Decision.
     *
     * @param decisionVersion - The DecisionVersion to deploy
     */
    void deploy(DecisionVersion decisionVersion);

    /**
     * Request deletion of the specific DecisionVersion.
     *
     * @param decisionVersion - The DecisionVersion to delete
     */
    void delete(DecisionVersion decisionVersion);

    /**
     * Request deletion of the specific Decision (and all versions)
     *
     * @param decision - The Decision to delete
     */
    void delete(Decision decision);
}
