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

package org.kie.baaas.dfm.app.dfs.selector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.baaas.dfm.app.dao.DecisionFleetShardDAO;
import org.kie.baaas.dfm.app.dfs.DecisionFleetShardSelector;
import org.kie.baaas.dfm.app.model.Decision;
import org.kie.baaas.dfm.app.model.DecisionFleetShard;

/**
 * Determines which Fleet Shard we will deploy the given Decision to.
 */
@ApplicationScoped
public class DefaultDecisionFleetShardSelector implements DecisionFleetShardSelector {

    private final DecisionFleetShardDAO decisionFleetShardDAO;

    @Inject
    public DefaultDecisionFleetShardSelector(DecisionFleetShardDAO decisionFleetShardDAO) {
        this.decisionFleetShardDAO = decisionFleetShardDAO;
    }

    public DecisionFleetShard selectFleetShardForDeployment(Decision decision) {
        return decisionFleetShardDAO.findById(DecisionFleetShardDAO.DEFAULT_DFS_ID);
    }
}
