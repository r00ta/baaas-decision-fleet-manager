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

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneClient;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;

/**
 * Creates a client to interact with the given ClusterControlPlane.
 */
@ApplicationScoped
public class ClusterControlPlaneClientFactory {

    public ClusterControlPlaneClient createClientFor(ClusterControlPlane clusterControlPlane) {

        Config config = new ConfigBuilder().withMasterUrl(clusterControlPlane.getKubernetesApiUrl()).build();
        KubernetesClient client = new DefaultKubernetesClient(config);

        return new DefaultClusterControlPlaneClient(client, clusterControlPlane);
    }
}
