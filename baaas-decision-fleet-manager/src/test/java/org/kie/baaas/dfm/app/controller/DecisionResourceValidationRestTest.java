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

package org.kie.baaas.dfm.app.controller;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.baaas.dfm.api.decisions.DecisionRequest;
import org.kie.baaas.dfm.api.decisions.Model;
import org.kie.baaas.dfm.app.manager.DecisionManager;
import org.kie.baaas.dfm.app.manager.validators.ValidationTestProfile;
import org.kie.baaas.dfm.app.model.DecisionVersion;
import org.kie.baaas.dfm.app.storage.DMNStorageRequest;
import org.kie.baaas.dfm.app.storage.DecisionDMNStorage;
import org.kie.baaas.dfm.app.storage.s3.S3DMNStorage;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;

import static io.restassured.RestAssured.given;
import static org.kie.baaas.dfm.app.TestConstants.DEFAULT_CUSTOMER_ID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(ValidationTestProfile.class)
public class DecisionResourceValidationRestTest {
    @Inject
    DecisionDMNStorage dmnStorage;

    @Inject
    DecisionManager decisionManager;

    @BeforeAll
    public static void beforeAll() {
        S3DMNStorage storage = Mockito.mock(S3DMNStorage.class);
        QuarkusMock.installMockForType(storage, S3DMNStorage.class);
    }

    private DMNStorageRequest createStorageRequest() {
        DMNStorageRequest request = new DMNStorageRequest("provider-url", "hash");
        when(dmnStorage.writeDMN(anyString(), Mockito.any(DecisionRequest.class), Mockito.any(DecisionVersion.class))).thenReturn(request);
        return request;
    }

    private DecisionRequest createApiRequest() {
        Model model = new Model();
        model.setDmn("<xml><test>\"hello\"</test></xml>");

        DecisionRequest decisions = new DecisionRequest();
        decisions.setDescription("The Best Decision Ever");
        decisions.setName("first-decision");
        decisions.setModel(model);
        decisions.setKind("Decision");
        return decisions;
    }

    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    @Test
    void testDecisionRequestValidator_exceedAllowedLimit() {
        //Max allowed decision is 1 for this test

        //Mock a storage for DMN
        createStorageRequest();

        //creates the first decision
        DecisionRequest apiRequest = createApiRequest();
        decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        //creates 2nd one through HTTP and fails at max allowed limit validator
        given().when()
                .contentType(MediaType.APPLICATION_JSON).body(apiRequest)
                .when().post("/decisions").then().statusCode(400);
    }

}
