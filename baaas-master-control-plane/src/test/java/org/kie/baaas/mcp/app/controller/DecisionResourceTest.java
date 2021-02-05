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

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.api.decisions.DecisionResponse;
import org.kie.baaas.mcp.api.decisions.Model;
import org.kie.baaas.mcp.api.decisions.ResponseModel;
import org.kie.baaas.mcp.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.mcp.app.manager.DecisionManager;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.resolvers.CustomerIdResolver;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class DecisionResourceTest {

    @Mock
    CustomerIdResolver customerIdResolver;

    @Mock
    DecisionManager decisionManager;

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
        lenient().when(customerIdResolver.getCustomerId()).thenReturn("customer-id");
    }

    @Test
    public void processValidRequest() {
        DecisionRequest decisionRequest = createApiRequest();

        lenient().when(decisionManager.createOrUpdateVersion("customer-id", decisionRequest)).thenReturn(decisionVersion);
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
