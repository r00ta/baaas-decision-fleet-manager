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

package org.kie.baaas.dfm.app.dao;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.baaas.dfm.app.config.DecisionFleetManagerConfig;
import org.kie.baaas.dfm.app.model.Decision;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DecisionDAOTest {

    @Inject
    DecisionFleetManagerConfig controlPlaneConfig;

    @Inject
    DecisionDAO decisionDAO;

    @TestTransaction
    @Test
    public void findByCustomerAndIdOrName_BAAAS_305() {
        Decision decision = new Decision();
        decision.setId("myId");
        decision.setCustomerId("jrota");
        decision.setName("myDecision");
        decisionDAO.persist(decision);
        Decision retrieved = decisionDAO.findByCustomerAndIdOrName("NOT VALID", "myId");
        Assertions.assertNull(retrieved);
    }
}
