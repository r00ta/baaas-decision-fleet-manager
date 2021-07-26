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

package org.kie.baaas.dfm.app.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.dfm.api.decisions.DecisionResponse;
import org.kie.baaas.dfm.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.dfm.app.model.Decision;
import org.kie.baaas.dfm.app.model.DecisionVersion;
import org.kie.baaas.dfm.app.model.DecisionVersionStatus;
import org.kie.baaas.dfm.app.model.deployment.Deployment;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;

import static org.kie.baaas.dfm.app.TestConstants.DEFAULT_CUSTOMER_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@ExtendWith(MockitoExtension.class)
public class AdminResourceTest {

    private static final String DECISION_ID = "id";

    @InjectMock
    DecisionLifecycleOrchestrator decisionLifecycle;

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID, roles = { "user" })
    public void unauthorized() {
        RestAssured.given()
                .get("/admin/decisions")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID, roles = { "admin" })
    public void deleteDecision() {

        Decision decision = mock(Decision.class);

        when(decisionLifecycle.deleteDecision(DECISION_ID)).thenReturn(decision);

        RestAssured.given()
                .delete("/admin/decisions/" + DECISION_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID, roles = { "admin" })
    public void deleteDecisionVersion() {
        Decision decision = createDecision();
        DecisionVersion version = decision.getCurrentVersion();

        when(decisionLifecycle.deleteVersion(decision.getId(), version.getVersion())).thenReturn(version);

        DecisionResponse response = RestAssured.given()
                .delete("/admin/decisions/" + decision.getId() + "/versions/" + version.getVersion())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DecisionResponse.class);

        Assertions.assertEquals(decision.getId(), response.getId());
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID, roles = { "admin" })
    public void listDecisions() {
        Decision decision = createDecision();
        List<Decision> listResult = Collections.singletonList(decision);

        when(decisionLifecycle.listDecisions()).thenReturn(listResult);

        List<DecisionResponse> decisions = Arrays.asList(RestAssured.given()
                .get("/admin/decisions")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DecisionResponse[].class));

        Assertions.assertEquals(1, decisions.size());
        Assertions.assertEquals(decision.getName(), decisions.get(0).getName());
    }

    private Decision createDecision() {
        DecisionVersion decisionVersion = new DecisionVersion();
        decisionVersion.setVersion(1L);
        decisionVersion.setDescription("description");
        decisionVersion.setStatus(DecisionVersionStatus.CURRENT);
        decisionVersion.setDeployment(new Deployment());

        Decision decision = new Decision();
        decision.setName("test");
        decision.setCustomerId(DEFAULT_CUSTOMER_ID);
        decision.setVersions(Collections.singletonList(decisionVersion));
        decision.setCurrentVersion(decisionVersion);

        decisionVersion.setDecision(decision);

        return decision;
    }
}
