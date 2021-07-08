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

package org.kie.baaas.dfm.app;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.baaas.dfm.app.dao.DecisionFleetShardDAO;
import org.kie.baaas.dfm.app.model.DecisionFleetShard;

import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class DBMigrationTest {

    @Inject
    DecisionFleetShardDAO decisionFleetShardDAO;

    @Test
    public void flyway_migrate() {

        DecisionFleetShard fleetShard = decisionFleetShardDAO.findById(DecisionFleetShardDAO.DEFAULT_DFS_ID);
        assertThat(fleetShard.getDmnJitUrl(), equalTo("https://baaas-dmn-jit-baaas-dmn-jit-demo.apps.kogito-cloud.automation.rhmw.io/jitdmn"));
        assertThat(fleetShard.getKubernetesApiUrl(), equalTo("https://kubernetes.default.svc"));
    }
}
