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

package org.kie.baaas.dfm.app.dfs.client;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.baaas.dfm.app.config.DecisionFleetManagerConfig;
import org.kie.baaas.dfm.app.dfs.DecisionFleetShardClient;
import org.kie.baaas.dfm.app.model.DecisionFleetShard;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import static java.util.Objects.requireNonNull;

/**
 * Creates a client to interact with the given Fleet Shard
 */
@ApplicationScoped
public class DecisionFleetShardClientFactory {

    private final DecisionFleetManagerConfig fmConfig;

    @Inject
    public DecisionFleetShardClientFactory(DecisionFleetManagerConfig config) {
        requireNonNull(config, "config cannot be null");
        this.fmConfig = config;
    }

    public DecisionFleetShardClient createClientFor(DecisionFleetShard fleetShard) {

        Config config = new ConfigBuilder().withMasterUrl(fleetShard.getKubernetesApiUrl()).build();
        KubernetesClient client = new DefaultKubernetesClient(config);

        return new DefaultDecisionFleetShardClient(this.fmConfig, client, fleetShard);
    }
}
