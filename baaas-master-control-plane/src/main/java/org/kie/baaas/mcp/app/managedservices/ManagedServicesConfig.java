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

package org.kie.baaas.mcp.app.managedservices;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.cloud.api.kas.invoker.ApiClient;

@ApplicationScoped
public class ManagedServicesConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedServicesConfig.class);

    private final ApiClient client = new ApiClient();

    public ManagedServicesConfig(@ConfigProperty(name = "baaas.mcp.managed-services.endpoint") Optional<String> basePath) {
        if (basePath.isPresent()) {
            client.setBasePath(basePath.get());
        }
        LOGGER.info("Configured Managed Services client {}", client.getBasePath());
    }

    public ApiClient getClient() {
        return client;
    }

}
