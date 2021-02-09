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

package org.kie.baaas.mcp.app.ccp.client;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneClient;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;

public class DefaultClusterControlPlaneClient implements ClusterControlPlaneClient {

    private KubernetesClient kubernetesClient;

    private ClusterControlPlane clusterControlPlane;

    public DefaultClusterControlPlaneClient(KubernetesClient kubernetesClient, ClusterControlPlane clusterControlPlane) {
        this.kubernetesClient = kubernetesClient;
        this.clusterControlPlane = clusterControlPlane;
    }

    @Override
    public ClusterControlPlane getClusterControlPlane() {
        return clusterControlPlane;
    }

    @Override
    public void deploy(DecisionVersion decisionVersion) {
        // Map the DecisionVersion to the CRD expected by the CCP and create it in the correct namespace
    }

    @Override
    public void rollback(DecisionVersion decisionVersion) {
        // Update the correct CRD in the customer namespace to adjust the pointer in the CCP for the current version
    }

    @Override
    public void delete(DecisionVersion decisionVersion) {
        // Delete the correct resource from the customer namespace of the CCP
    }

    @Override
    public void delete(Decision decision) {
        // Delete the entire Decision resource from the correct customer namespace on the CCP
    }
}
