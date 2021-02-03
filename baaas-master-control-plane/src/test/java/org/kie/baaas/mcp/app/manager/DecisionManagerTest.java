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

package org.kie.baaas.mcp.app.manager;

import javax.inject.Inject;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.api.decisions.DecisionsResponse;
import org.kie.baaas.mcp.api.decisions.ResponseModel;
import org.kie.baaas.mcp.api.eventing.Eventing;
import org.kie.baaas.mcp.api.eventing.kafka.Kafka;
import org.kie.baaas.mcp.app.dao.DecisionDAO;
import org.kie.baaas.mcp.app.dao.DecisionVersionDAO;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.DecisionVersionStatus;
import org.kie.baaas.mcp.app.model.eventing.KafkaTopics;
import org.kie.baaas.mcp.app.resolvers.CustomerIdResolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class DecisionManagerTest {

    @Inject
    CustomerIdResolver customerIdResolver;

    @Inject
    DecisionDAO decisionDAO;

    @Inject
    DecisionVersionDAO decisionVersionDAO;

    @Inject
    DecisionManager decisionManager;

    private DecisionsResponse createApiResponse() {
        ResponseModel model = new ResponseModel();
        // model.setDmn("dmnnn");
        model.setMd5("md555");

        DecisionsResponse decisions = new DecisionsResponse();
        decisions.setDescription("The Best Decision Ever");
        decisions.setName("robs-first-decision");
        decisions.setResponseModel(model);
        return decisions;
    }

    private DecisionsResponse createApiResponseWithVersion(long version) {
        DecisionsResponse apiResponse = createApiResponse();
        apiResponse.setVersion(version);
        return apiResponse;
    }

    @TestTransaction
    @Test
    public void createNewVersion_withKafka() {

        Kafka kafka = new Kafka();
        kafka.setSink("my-sink");
        kafka.setSource("my-source");

        Eventing eventing = new Eventing();
        eventing.setKafka(kafka);

        DecisionsResponse apiResponse = createApiResponse();
        apiResponse.setEventing(eventing);

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        KafkaTopics kafkaTopics = decision.getNextVersion().getKafkaTopics();
        assertThat(kafkaTopics.getSinkTopic(), equalTo(kafka.getSink()));
        assertThat(kafkaTopics.getSourceTopic(), equalTo(kafka.getSource()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_newDecision() {

        DecisionsResponse apiResponse = createApiResponse();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        assertThat(decision, is(notNullValue()));

        assertThat(decision.getName(), equalTo(apiResponse.getName()));
        assertThat(decision.getDescription(), equalTo(apiResponse.getDescription()));
        assertThat(decision.getCustomerId(), equalTo(customerIdResolver.getCustomerId()));

        assertThat(decision.getNextVersion().getVersion(), equalTo(1L));
        assertThat(decision.getNextVersion().getStatus(), equalTo(DecisionVersionStatus.BUILDING));
        assertThat(decision.getNextVersion().getSubmittedAt(), is(notNullValue()));
        // TODO should point to s3 bucket location?
        // assertThat(decision.getNextVersion().getDmnMd5(), equalTo(apiResponse.getModel().getMd5()));
    }

    @TestTransaction
    @Test
    public void deployed_withFirstVersionOfDecision() {

        DecisionsResponse apiResponse = createApiResponse();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        Decision deployed = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        assertThat(deployed, is(notNullValue()));
        assertThat(deployed.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(deployed.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void failed_withFirstVersionOfDecision() {

        DecisionsResponse apiResponse = createApiResponse();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        Decision deployed = decisionManager.failed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        assertThat(deployed, is(notNullValue()));
        assertThat(deployed.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.FAILED));
        assertThat(deployed.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateFailsWhenStillCreating() {

        DecisionsResponse apiResponse = createApiResponse();

        decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);

        apiResponse.setDescription("An updated version!");
        // apiResponse.getResponseModel().setDmn("fff");
        apiResponse.getResponseModel().setMd5("sdsdsd");

        DecisionLifecycleException thrown = assertThrows(DecisionLifecycleException.class, () ->
                decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse));

        assertThat(thrown.getMessage(), equalTo("A lifecycle operation is already in progress for Version '1' of Decision 'robs-first-decision'"));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecision() {

        DecisionsResponse apiResponse = createApiResponse();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        apiResponse.setDescription("An updated version!");
        // apiResponse.getModel().setDmn("fff");
        apiResponse.getResponseModel().setMd5("sdsdsd");

        decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getNextVersion().getStatus(), equalTo(DecisionVersionStatus.BUILDING));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecisionUpdateSuccess() {

        DecisionsResponse apiResponse = createApiResponse();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        apiResponse.setDescription("An updated version!");
        // apiResponse.getModel().setDmn("fff");
        apiResponse.getResponseModel().setMd5("sdsdsd");

        decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        long nextVersion = decision.getNextVersion().getVersion();

        decision = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(nextVersion));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecisionUpdateFailed() {

        DecisionsResponse apiResponse = createApiResponse();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        apiResponse.setDescription("An updated version!");
        // apiResponse.getModel().setDmn("fff");
        apiResponse.getResponseModel().setMd5("sdsdsd");

        decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        long currentVersion = decision.getCurrentVersion().getVersion();

        decision = decisionManager.failed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(currentVersion));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @Test
    public void deleteDecision() {

        DecisionsResponse apiResponse = createApiResponse();
        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        decision = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        String versionId = decision.getCurrentVersion().getId();

        decisionManager.deleteDecision(decision.getCustomerId(), apiResponse);

        assertThat(decisionDAO.findById(decision.getId()), is(nullValue()));
        assertThat(decisionVersionDAO.findById(versionId), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void deleteVersion_versionDoesNotExist() {

        DecisionsResponse apiResponse = createApiResponse();

        assertThrows(DecisionLifecycleException.class, () -> decisionManager.deleteVersion(customerIdResolver.getCustomerId(), apiResponse));
    }

    @TestTransaction
    @Test
    public void deleteVersion() {

        DecisionsResponse apiResponse = createApiResponse();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        decisionManager.deployed(customerIdResolver.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        //apiResponse.getModel().setMd5("foososo");
        decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        decision = decisionManager.deployed(customerIdResolver.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        assertThat(decision.getCurrentVersion().getVersion(), equalTo(2L));
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));

        DecisionsResponse deleteRequest = createApiResponseWithVersion(1L);
        DecisionVersion deletedVersion = decisionManager.deleteVersion(customerIdResolver.getCustomerId(), deleteRequest);
        assertThat(deletedVersion.getStatus(), equalTo(DecisionVersionStatus.DELETED));
    }

    @Test
    @TestTransaction
    public void deleteVersion_versionIsCurrentVersion() {

        DecisionsResponse apiResponse = createApiResponse();
        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        decision = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getCurrentVersion().getVersion());

        DecisionsResponse deleteRequest = createApiResponseWithVersion(decision.getCurrentVersion().getVersion());

        assertThrows(DecisionLifecycleException.class, () -> decisionManager.deleteVersion(customerIdResolver.getCustomerId(), deleteRequest));
    }

    @Test
    @TestTransaction
    public void deleteVersion_canDeleteAFailedVersion() {
        DecisionsResponse apiResponse = createApiResponse();
        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        decision = decisionManager.failed(customerIdResolver.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        DecisionsResponse deleteRequest = createApiResponseWithVersion(decision.getCurrentVersion().getVersion());
        DecisionVersion decisionVersion = decisionManager.deleteVersion(customerIdResolver.getCustomerId(), deleteRequest);

        assertThat(decisionVersion.getStatus(), equalTo(DecisionVersionStatus.DELETED));
        assertThat(decisionVersion.getDecision().getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.DELETED));
    }

    @TestTransaction
    @Test
    public void deleteVersion_versionInvolvedInLifecycleOperation() {
        DecisionsResponse apiResponse = createApiResponse();
        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        DecisionsResponse deleteRequest = createApiResponseWithVersion(decision.getCurrentVersion().getVersion());

        assertThrows(DecisionLifecycleException.class, () -> decisionManager.deleteVersion(customerIdResolver.getCustomerId(), deleteRequest));
    }
}
