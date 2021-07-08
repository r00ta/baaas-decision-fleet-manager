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

package org.kie.baaas.dfm.app.dfs;

import org.kie.baaas.dfm.app.model.Decision;
import org.kie.baaas.dfm.app.model.DecisionFleetShard;

/**
 * Interface for implementations that determine to which Fleet Shard a Decision
 * should be deployed.
 */
public interface DecisionFleetShardSelector {

    /**
     * Selects which FleetShard should receive the deployment for the specified Decision
     * 
     * @param decision - The Decision that is being deployed
     * @return - The FleetShard that the Decision should be deployed to
     */
    DecisionFleetShard selectFleetShardForDeployment(Decision decision);
}
