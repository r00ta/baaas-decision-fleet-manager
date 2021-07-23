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

package org.kie.baaas.dfm.app.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Stores the configuration of the single Fleet Shard that is allowed to be registered for the
 * current implementation of the Fleet Manager.
 */
@ApplicationScoped
public class DecisionFleetManagerConfig {

    @NotBlank
    @ConfigProperty(name = "baaas.dfm.urls.api-base")
    String apiBaseUrl;

    @NotBlank
    @ConfigProperty(name = "baaas.dfs.urls.dmn-jit")
    String dfsDmnJitUrl;

    @NotBlank
    @ConfigProperty(name = "baaas.dfs.urls.k8s-api")
    String dfsKubernetesApiUrl;

    @NotBlank
    @ConfigProperty(name = "baaas.dfs.namespace")
    String dfsNamespace;

    @Inject
    @ConfigProperty(name = "baaas.dfm.s3.bucket.name")
    String bucketName;

    @Inject
    @ConfigProperty(name = "baaas.dfm.max.allowed.decisions")
    long maxAllowedDecisions = -1; //disabled by default

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getDfsDmnJitUrl() {
        return dfsDmnJitUrl;
    }

    public String getDfsKubernetesApiUrl() {
        return dfsKubernetesApiUrl;
    }

    public String getDfsNamespace() {
        return dfsNamespace;
    }

    public String getBucketName() {
        return bucketName;
    }

    public long getMaxAllowedDecisions() {
        return maxAllowedDecisions;
    }
}
