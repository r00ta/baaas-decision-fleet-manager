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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.dfm.api.decisions.DecisionRequest;
import org.kie.baaas.dfm.api.decisions.DecisionResponse;
import org.kie.baaas.dfm.api.decisions.DecisionResponseList;
import org.kie.baaas.dfm.api.decisions.Model;
import org.kie.baaas.dfm.api.decisions.ResponseModel;
import org.kie.baaas.dfm.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.dfm.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.dfm.app.model.Decision;
import org.kie.baaas.dfm.app.model.DecisionVersion;
import org.kie.baaas.dfm.app.model.ListResult;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.kie.baaas.dfm.app.TestConstants.DEFAULT_CUSTOMER_ID;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@ExtendWith(MockitoExtension.class)
public class DecisionResourceTest {

    private static final String DECISION_ID = "id";

    @InjectMock
    DecisionLifecycleOrchestrator decisionLifecycle;

    DecisionVersion decisionVersion;

    @InjectMock
    DecisionMapper decisionMapper;

    private DecisionRequest buildDecisionRequest() {
        Model model = new Model();
        model.setDmn("<xml test=\"123\">foo</xml>");

        DecisionRequest request = new DecisionRequest();
        request.setKind("Decision");
        request.setDescription("The Best Decision Ever");
        request.setName("robs-first-decision");
        request.setModel(model);
        return request;
    }

    private DecisionResponse buildDecisionResponse() {
        ResponseModel model = new ResponseModel();
        model.setHref("href");
        model.setMd5("md5");

        DecisionResponse response = new DecisionResponse();
        response.setDescription("The Best Decision Ever");
        response.setName("robs-first-decision");
        response.setId(DECISION_ID);
        response.setResponseModel(model);
        response.setVersion(100L);
        return response;
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    @Disabled("https://issues.redhat.com/browse/BAAAS-156")
    public void rollbackToVersion() {
        DecisionVersion version = mock(DecisionVersion.class);
        DecisionResponse decisionResponse = buildDecisionResponse();
        long decisionVersion = 1L;

        when(decisionLifecycle.setCurrentVersion(DEFAULT_CUSTOMER_ID, DECISION_ID, decisionVersion)).thenReturn(version);
        when(decisionMapper.mapVersionToDecisionResponse(version)).thenReturn(decisionResponse);

        DecisionResponse decision = RestAssured.given()
                .put("/decisions/" + DECISION_ID + "/versions/" + decisionVersion)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DecisionResponse.class);

        assertThat(decision, equalTo(decisionResponse));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void deleteDecision() {

        Decision decision = mock(Decision.class);

        when(decisionLifecycle.deleteDecision(DEFAULT_CUSTOMER_ID, DECISION_ID)).thenReturn(decision);

        RestAssured.given()
                .delete("/decisions/" + DECISION_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void deleteDecisionVersion() {
        DecisionVersion version = mock(DecisionVersion.class);
        DecisionResponse decisionResponse = buildDecisionResponse();
        long decisionVersion = 1L;

        when(decisionLifecycle.deleteVersion(DEFAULT_CUSTOMER_ID, DECISION_ID, decisionVersion)).thenReturn(version);
        when(decisionMapper.mapVersionToDecisionResponse(version)).thenReturn(decisionResponse);

        DecisionResponse decision = RestAssured.given()
                .delete("/decisions/" + DECISION_ID + "/versions/" + decisionVersion)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DecisionResponse.class);

        assertThat(decision, equalTo(decisionResponse));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getDecisionVersionDMN() {

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.writeBytes("<xml test=\"123\">foo</xml>".getBytes(StandardCharsets.UTF_8));

        long version = 1L;

        when(decisionLifecycle.getDMN(DEFAULT_CUSTOMER_ID, DECISION_ID, version)).thenReturn(b);

        RestAssured.given()
                .get("/decisions/" + DECISION_ID + "/versions/" + version + "/dmn")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(containsString("123"));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getDecisionVersion() {
        DecisionVersion version = mock(DecisionVersion.class);
        DecisionResponse decisionResponse = buildDecisionResponse();
        long decisionVersion = 1L;

        when(decisionLifecycle.getVersion(DEFAULT_CUSTOMER_ID, DECISION_ID, decisionVersion)).thenReturn(version);
        when(decisionMapper.mapVersionToDecisionResponse(version)).thenReturn(decisionResponse);

        DecisionResponse decision = RestAssured.given()
                .get("/decisions/" + DECISION_ID + "/versions/" + decisionVersion)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DecisionResponse.class);

        assertThat(decision, equalTo(decisionResponse));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getBuildingVersion() {
        DecisionVersion version = mock(DecisionVersion.class);
        DecisionResponse decisionResponse = buildDecisionResponse();

        when(decisionLifecycle.getBuildingVersion(DEFAULT_CUSTOMER_ID, DECISION_ID)).thenReturn(version);
        when(decisionMapper.mapVersionToDecisionResponse(version)).thenReturn(decisionResponse);

        DecisionResponse decision = RestAssured.given()
                .get("/decisions/" + DECISION_ID + "/building")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DecisionResponse.class);

        assertThat(decision, equalTo(decisionResponse));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getDecision() {
        DecisionVersion version = mock(DecisionVersion.class);
        DecisionResponse decisionResponse = buildDecisionResponse();

        when(decisionLifecycle.getCurrentVersion(DEFAULT_CUSTOMER_ID, DECISION_ID)).thenReturn(version);
        when(decisionMapper.mapVersionToDecisionResponse(version)).thenReturn(decisionResponse);

        DecisionResponse decision = RestAssured.given()
                .get("/decisions/" + DECISION_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DecisionResponse.class);

        assertThat(decision, equalTo(decisionResponse));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void listDecisionVersions() {

        ListResult<DecisionVersion> versions = mock(ListResult.class);
        DecisionResponseList responseList = mock(DecisionResponseList.class);
        ArgumentCaptor<ListResult<DecisionVersion>> decisionList = ArgumentCaptor.forClass(ListResult.class);
        int page = 0;
        int size = 100;

        when(decisionLifecycle.listDecisionVersions(DEFAULT_CUSTOMER_ID, DECISION_ID, page, size)).thenReturn(versions);
        when(decisionMapper.mapVersionsToDecisionResponseList(decisionList.capture())).thenReturn(responseList);

        DecisionResponseList decisions = RestAssured.given()
                .get("/decisions/" + DECISION_ID + "/versions")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DecisionResponseList.class);

        assertThat(responseList.getItems().size(), equalTo(decisions.getItems().size()));
        assertThat(responseList.getKind(), is(responseList.getKind()));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void listDecisions() {

        ListResult<Decision> listResult = mock(ListResult.class);
        DecisionResponseList responseList = mock(DecisionResponseList.class);
        ArgumentCaptor<ListResult<Decision>> decisionList = ArgumentCaptor.forClass(ListResult.class);

        int page = 0;
        int size = 100;

        when(decisionLifecycle.listDecisions(DEFAULT_CUSTOMER_ID, page, size)).thenReturn(listResult);
        when(decisionMapper.mapToDecisionResponseList(decisionList.capture())).thenReturn(responseList);

        DecisionResponseList decisions = RestAssured.given()
                .get("/decisions")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DecisionResponseList.class);

        assertThat(responseList.getItems().size(), equalTo(decisions.getItems().size()));
        assertThat(responseList.getKind(), is(responseList.getKind()));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createOrUpdateDecision() {
        DecisionRequest decisionRequest = buildDecisionRequest();

        lenient().when(decisionLifecycle.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, decisionRequest)).thenReturn(decisionVersion);
        lenient().when(decisionMapper.mapVersionToDecisionResponse(decisionVersion)).thenReturn(buildDecisionResponse());
        DecisionResponse decisionResponse = RestAssured.given()
                .body(decisionRequest)
                .contentType(ContentType.JSON)
                .post("/decisions")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .as(DecisionResponse.class);

        assertThat(decisionResponse.getResponseModel().getHref(), equalTo("href"));
        assertThat(decisionResponse.getResponseModel().getMd5(), equalTo("md5"));
        assertThat(decisionResponse.getId(), equalTo(DECISION_ID));
        assertThat(decisionResponse.getVersion(), equalTo(100L));
    }
}
