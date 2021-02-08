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

package org.kie.baaas.mcp.app.controller;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.api.decisions.DecisionResponse;
import org.kie.baaas.mcp.api.decisions.DecisionResponseList;
import org.kie.baaas.mcp.api.decisions.Model;
import org.kie.baaas.mcp.api.decisions.ResponseModel;
import org.kie.baaas.mcp.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.mcp.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.resolvers.CustomerIdResolver;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DecisionResourceTest {

    private static final String DEFAULT_CUSTOMER_ID = "customer-id";

    @Mock
    CustomerIdResolver customerIdResolver;

    @Mock
    DecisionLifecycleOrchestrator decisionLifecycle;

    @Mock
    DecisionVersion decisionVersion;

    @Mock
    DecisionMapper decisionMapper;

    @InjectMocks
    private DecisionResource decisionResource;

    private DecisionRequest createApiRequest() {
        Model model = new Model();
        model.setDmn("<xml test=\"123\">foo</xml>");

        DecisionRequest request = new DecisionRequest();
        request.setDescription("The Best Decision Ever");
        request.setName("robs-first-decision");
        request.setModel(model);
        return request;
    }

    private DecisionResponse createApiResponse() {
        ResponseModel model = new ResponseModel();
        model.setHref("href");
        model.setMd5("md5");

        DecisionResponse response = new DecisionResponse();
        response.setDescription("The Best Decision Ever");
        response.setName("robs-first-decision");
        response.setId("id");
        response.setResponseModel(model);
        response.setVersion(100L);
        return response;
    }

    @BeforeEach
    public void beforeEach() {
        lenient().when(customerIdResolver.getCustomerId()).thenReturn(DEFAULT_CUSTOMER_ID);
    }

    @Test
    public void rollbackToVersion() {
        DecisionVersion version = mock(DecisionVersion.class);
        DecisionResponse decisionResponse = mock(DecisionResponse.class);
        String decisionId = "foo";
        long decisionVersion = 1l;

        when(decisionLifecycle.rollbackToVersion(DEFAULT_CUSTOMER_ID, decisionId, decisionVersion)).thenReturn(version);
        when(decisionMapper.mapVersionToDecisionResponse(version)).thenReturn(decisionResponse);

        Response response = decisionResource.rollbackToDecisionVersion(decisionId, decisionVersion);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        assertThat(decisionResponse, equalTo(response.readEntity(DecisionResponse.class)));
    }

    @Test
    public void deleteDecision() {

        Decision decision = mock(Decision.class);
        DecisionResponse decisionResponse = mock(DecisionResponse.class);
        String decisionId = "foo";

        when(decisionLifecycle.deleteDecision(DEFAULT_CUSTOMER_ID, decisionId)).thenReturn(decision);
        when(decisionMapper.mapToDecisionResponse(decision)).thenReturn(decisionResponse);

        Response response = decisionResource.deleteDecision(decisionId);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        assertThat(decisionResponse, equalTo(response.readEntity(DecisionResponse.class)));
    }

    @Test
    public void deleteDecisionVersion() {
        DecisionVersion version = mock(DecisionVersion.class);
        DecisionResponse decisionResponse = mock(DecisionResponse.class);
        String decisionId = "foo";
        long decisionVersion = 1l;

        when(decisionLifecycle.deleteVersion(DEFAULT_CUSTOMER_ID, decisionId, decisionVersion)).thenReturn(version);
        when(decisionMapper.mapVersionToDecisionResponse(version)).thenReturn(decisionResponse);

        Response response = decisionResource.deleteDecisionVersion(decisionId, decisionVersion);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        assertThat(decisionResponse, equalTo(response.readEntity(DecisionResponse.class)));
    }

    @Test
    public void getDecisionVersionDMN() {

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.writeBytes("<xml test=\"123\">foo</xml>".getBytes(StandardCharsets.UTF_8));

        when(decisionLifecycle.getDMNFromBucket("customer-id", "foo" ,1L)).thenReturn(b);

        Response response = decisionResource.getDecisionVersionDMN("foo", 1L);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), equalTo(true));
    }


    @Test
    public void getDecisionVersion() {
        DecisionVersion version = mock(DecisionVersion.class);
        DecisionResponse decisionResponse = mock(DecisionResponse.class);
        String decisionId = "foo";
        long decisionVersion = 1l;

        when(decisionLifecycle.getVersion(DEFAULT_CUSTOMER_ID, decisionId, decisionVersion)).thenReturn(version);
        when(decisionMapper.mapVersionToDecisionResponse(version)).thenReturn(decisionResponse);

        Response response = decisionResource.getDecisionVersion(decisionId, decisionVersion);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        assertThat(decisionResponse, equalTo(response.readEntity(DecisionResponse.class)));
    }

    @Test
    public void getDecision() {
        DecisionVersion version = mock(DecisionVersion.class);
        DecisionResponse decisionResponse = mock(DecisionResponse.class);
        String decisionId = "foo";

        when(decisionLifecycle.getCurrentVersion(DEFAULT_CUSTOMER_ID, decisionId)).thenReturn(version);
        when(decisionMapper.mapVersionToDecisionResponse(version)).thenReturn(decisionResponse);

        Response response = decisionResource.getDecision(decisionId);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        assertThat(decisionResponse, equalTo(response.readEntity(DecisionResponse.class)));
    }

    @Test
    public void listDecisionVersions() {

        DecisionVersion version = mock(DecisionVersion.class);
        DecisionResponseList responseList = mock(DecisionResponseList.class);
        ArgumentCaptor<List<DecisionVersion>> decisionList = ArgumentCaptor.forClass(List.class);

        String decisionId = "foo";

        when(decisionLifecycle.listDecisionVersions(DEFAULT_CUSTOMER_ID, decisionId)).thenReturn(singletonList(version));
        when(decisionMapper.mapVersionsToDecisionResponseList(decisionList.capture())).thenReturn(responseList);

        Response response = decisionResource.listDecisionVersions(decisionId);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        DecisionResponseList decisions = response.readEntity(DecisionResponseList.class);
        assertThat(responseList, equalTo(decisions));
        assertThat(decisionList.getValue(), contains(version));
    }

    @Test
    public void listDecisions() {

        Decision decision = mock(Decision.class);
        DecisionResponseList responseList = mock(DecisionResponseList.class);
        ArgumentCaptor<List<Decision>> decisionList = ArgumentCaptor.forClass(List.class);

        when(decisionLifecycle.listDecisions(DEFAULT_CUSTOMER_ID)).thenReturn(singletonList(decision));
        when(decisionMapper.mapToDecisionResponseList(decisionList.capture())).thenReturn(responseList);

        Response response = decisionResource.listDecisions();
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        DecisionResponseList decisions = response.readEntity(DecisionResponseList.class);
        assertThat(responseList, equalTo(decisions));
        assertThat(decisionList.getValue(), contains(decision));
    }

    @Test
    public void createOrUpdateDecision() {
        DecisionRequest decisionRequest = createApiRequest();

        lenient().when(decisionLifecycle.createOrUpdateVersion("customer-id", decisionRequest)).thenReturn(decisionVersion);
        lenient().when(decisionMapper.mapVersionToDecisionResponse(decisionVersion)).thenReturn(createApiResponse());

        Response response = decisionResource.createOrUpdateDecision(decisionRequest);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));

        DecisionResponse decisionResponse = response.readEntity(DecisionResponse.class);
        assertThat(decisionResponse.getResponseModel().getHref(), equalTo("href"));
        assertThat(decisionResponse.getResponseModel().getMd5(), equalTo("md5"));
        assertThat(decisionResponse.getId(), equalTo("id"));
        assertThat(decisionResponse.getVersion(), equalTo(100L));
    }
}
