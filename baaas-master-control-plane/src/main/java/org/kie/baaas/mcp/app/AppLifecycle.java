/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.baaas.mcp.app;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.kie.baaas.mcp.app.dao.DecisionFleetShardDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class AppLifecycle {

    private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());
    private final String componentName = "Master Control Plane";

    private final DecisionFleetShardDAO decisionFleetShardDAO;

    @Inject
    public AppLifecycle(DecisionFleetShardDAO decisionFleetShardDAO) {
        Objects.requireNonNull(decisionFleetShardDAO, "fleetShardDAO cannot be null");
        this.decisionFleetShardDAO = decisionFleetShardDAO;
    }

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("{} is starting and will be ready to process requests.", componentName);
        decisionFleetShardDAO.init();
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("{} is stopping, requests will be no longer processed.", componentName);
    }
}
