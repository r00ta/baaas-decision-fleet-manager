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

package org.kie.baaas.mcp.app.dfs.selector;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.app.dao.DecisionFleetShardDAO;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionFleetShard;
import org.mockito.Mock;

import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class DefaultDecisionDecisionFleetShardSelectorTest {

    @Mock
    private Decision decision;

    @Inject
    DefaultDecisionFleetShardSelector selector;

    @Test
    public void selectFleetShardForDeployment() {

        DecisionFleetShard planeForDeployment = selector.selectFleetShardForDeployment(decision);
        assertThat(planeForDeployment, is(notNullValue()));
        assertThat(planeForDeployment.getId(), equalTo(DecisionFleetShardDAO.DEFAULT_DFS_ID));
    }
}
