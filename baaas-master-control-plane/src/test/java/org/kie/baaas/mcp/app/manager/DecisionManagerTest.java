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
import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.api.decisions.Model;
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

    private DecisionRequest createApiRequest() {
        Model model = new Model();
        model.setDmn("foo");

        DecisionRequest decisions = new DecisionRequest();
        decisions.setDescription("The Best Decision Ever");
        decisions.setName("robs-first-decision");
        decisions.setModel(model);
        return decisions;
    }

    @TestTransaction
    @Test
    public void createNewVersion_withKafka() {

        Kafka kafka = new Kafka();
        kafka.setSink("my-sink");
        kafka.setSource("my-source");

        Eventing eventing = new Eventing();
        eventing.setKafka(kafka);

        DecisionRequest apiResponse = createApiRequest();
        apiResponse.setEventing(eventing);

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiResponse);
        KafkaTopics kafkaTopics = decisionVersion.getKafkaTopics();
        assertThat(kafkaTopics.getSinkTopic(), equalTo(kafka.getSink()));
        assertThat(kafkaTopics.getSourceTopic(), equalTo(kafka.getSource()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_newDecision() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        assertThat(decisionVersion, is(notNullValue()));

        Decision decision = decisionVersion.getDecision();

        assertThat(decision.getName(), equalTo(apiRequest.getName()));
        assertThat(decision.getDescription(), equalTo(apiRequest.getDescription()));
        assertThat(decision.getCustomerId(), equalTo(customerIdResolver.getCustomerId()));

        assertThat(decisionVersion.getVersion(), equalTo(1L));
        assertThat(decisionVersion.getStatus(), equalTo(DecisionVersionStatus.BUILDING));
        assertThat(decisionVersion.getSubmittedAt(), is(notNullValue()));
        assertThat(decisionVersion.getDmnMd5(), is(notNullValue()));
        assertThat(decisionVersion.getDmnLocation(), is(notNullValue()));
    }

    @TestTransaction
    @Test
    public void deployed_withFirstVersionOfDecision() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        DecisionVersion deployed = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion());

        assertThat(deployed, is(notNullValue()));
        assertThat(deployed.getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(deployed.getDecision().getCurrentVersion().getVersion(), equalTo(deployed.getVersion()));
        assertThat(deployed.getDecision().getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void failed_withFirstVersionOfDecision() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        DecisionVersion deployed = decisionManager.failed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion());
        assertThat(deployed, is(notNullValue()));
        assertThat(deployed.getStatus(), equalTo(DecisionVersionStatus.FAILED));

        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.FAILED));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateFailsWhenStillCreating() {

        DecisionRequest apiRequest = createApiRequest();

        decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("Updated dmn!");

        DecisionLifecycleException thrown = assertThrows(DecisionLifecycleException.class, () ->
                decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest));

        assertThat(thrown.getMessage(), equalTo("A lifecycle operation is already in progress for Version '1' of Decision 'robs-first-decision'"));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecision() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("Updated dmn!");

        decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        assertThat(decisionVersion.getStatus(), equalTo(DecisionVersionStatus.BUILDING));
        assertThat(decisionVersion.getVersion(), equalTo(2l));

        decision = decisionVersion.getDecision();
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getNextVersion().getStatus(), equalTo(DecisionVersionStatus.BUILDING));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecisionUpdateSuccess() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");

        decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionVersion = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion());
        assertThat(decisionVersion.getStatus(), equalTo(DecisionVersionStatus.CURRENT));

        decision = decisionVersion.getDecision();
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(decisionVersion.getVersion()));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecisionUpdateFailed() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();
        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");

        DecisionVersion failedVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        failedVersion = decisionManager.failed(decision.getCustomerId(), decision.getName(), failedVersion.getVersion());
        assertThat(failedVersion.getStatus(), equalTo(DecisionVersionStatus.FAILED));

        decision = failedVersion.getDecision();
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(decisionVersion.getVersion()));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @Test
    public void deleteDecision() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionVersion = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion());
        decisionManager.deleteDecision(decision.getCustomerId(), decision.getName());

        assertThat(decisionDAO.findById(decisionVersion.getId()), is(nullValue()));
        assertThat(decisionVersionDAO.findById(decisionVersion.getId()), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void deleteVersion_versionDoesNotExist() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        assertThrows(DecisionLifecycleException.class, () -> decisionManager.deleteVersion(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion() + 1l));
    }

    @TestTransaction
    @Test
    public void deleteVersion() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();
        decisionManager.deployed(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion());

        apiRequest.getModel().setDmn("updated dmn!");

        decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionVersion = decisionManager.deployed(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion());
        decision = decisionVersion.getDecision();

        assertThat(decision.getCurrentVersion().getVersion(), equalTo(2L));
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));

        DecisionVersion deletedVersion = decisionManager.deleteVersion(customerIdResolver.getCustomerId(), decision.getName(), 1l);
        assertThat(deletedVersion.getStatus(), equalTo(DecisionVersionStatus.DELETED));
    }

    @Test
    @TestTransaction
    public void deleteVersion_versionIsCurrentVersion() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        DecisionVersion deployed = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion());
        assertThrows(DecisionLifecycleException.class, () -> decisionManager.deleteVersion(customerIdResolver.getCustomerId(), decision.getName(), deployed.getVersion()));
    }

    @Test
    @TestTransaction
    public void deleteVersion_canDeleteAFailedVersion() {

        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionVersion = decisionManager.failed(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion());

        decisionVersion = decisionManager.deleteVersion(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion());
        assertThat(decisionVersion.getStatus(), equalTo(DecisionVersionStatus.DELETED));
        assertThat(decisionVersion.getDecision().getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.DELETED));
    }

    @TestTransaction
    @Test
    public void deleteVersion_versionInvolvedInLifecycleOperation() {
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        assertThrows(DecisionLifecycleException.class, () -> decisionManager.deleteVersion(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion()));
    }
}
