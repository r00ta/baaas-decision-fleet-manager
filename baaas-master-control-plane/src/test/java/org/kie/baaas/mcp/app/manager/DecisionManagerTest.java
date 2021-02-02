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
import org.kie.baaas.mcp.api.Decisions;
import org.kie.baaas.mcp.api.Eventing;
import org.kie.baaas.mcp.api.Kafka;
import org.kie.baaas.mcp.api.Model;
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
    private CustomerIdResolver customerIdResolver;

    @Inject
    private DecisionDAO decisionDAO;

    @Inject
    private DecisionVersionDAO decisionVersionDAO;

    @Inject
    private DecisionManager decisionManager;

    private Decisions createApiRequest() {
        Model model = new Model();
        model.setDmn("dmnnn");
        model.setMd5("md555");

        Decisions decisions = new Decisions();
        decisions.setDescription("The Best Decision Ever");
        decisions.setName("robs-first-decision");
        decisions.setModel(model);
        return decisions;
    }

    private Decisions createApiRequestWithVersion(long version) {
        Decisions apiRequest = createApiRequest();
        apiRequest.setVersion(version);
        return apiRequest;
    }

    @TestTransaction
    @Test
    public void createNewVersion_withKafka() {

        Kafka kafka = new Kafka();
        kafka.setSink("my-sink");
        kafka.setSource("my-source");

        Eventing eventing = new Eventing();
        eventing.setKafka(kafka);

        Decisions apiRequest = createApiRequest();
        apiRequest.setEventing(eventing);

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        KafkaTopics kafkaTopics = decision.getNextVersion().getKafkaTopics();
        assertThat(kafkaTopics.getSinkTopic(), equalTo(kafka.getSink()));
        assertThat(kafkaTopics.getSourceTopic(), equalTo(kafka.getSource()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_newDecision() {

        Decisions apiRequest = createApiRequest();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        assertThat(decision, is(notNullValue()));

        assertThat(decision.getName(), equalTo(apiRequest.getName()));
        assertThat(decision.getDescription(), equalTo(apiRequest.getDescription()));
        assertThat(decision.getCustomerId(), equalTo(customerIdResolver.getCustomerId()));

        assertThat(decision.getNextVersion().getVersion(), equalTo(1l));
        assertThat(decision.getNextVersion().getStatus(), equalTo(DecisionVersionStatus.BUILDING));
        assertThat(decision.getNextVersion().getSubmittedAt(), is(notNullValue()));
        assertThat(decision.getNextVersion().getDmnMd5(), equalTo(apiRequest.getModel().getMd5()));
    }

    @TestTransaction
    @Test
    public void deployed_withFirstVersionOfDecision() {

        Decisions apiRequest = createApiRequest();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision deployed = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        assertThat(deployed, is(notNullValue()));
        assertThat(deployed.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(deployed.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void failed_withFirstVersionOfDecision() {

        Decisions apiRequest = createApiRequest();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision deployed = decisionManager.failed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        assertThat(deployed, is(notNullValue()));
        assertThat(deployed.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.FAILED));
        assertThat(deployed.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateFailsWhenStillCreating() {

        Decisions apiRequest = createApiRequest();

        decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");
        apiRequest.getModel().setMd5("sdsdsd");

        DecisionLifecycleException thrown = assertThrows(DecisionLifecycleException.class, () -> {
            decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        });

        assertThat(thrown.getMessage(), equalTo("A lifecycle operation is already in progress for Version '1' of Decision 'robs-first-decision'"));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecision() {

        Decisions apiRequest = createApiRequest();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");
        apiRequest.getModel().setMd5("sdsdsd");

        decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getNextVersion().getStatus(), equalTo(DecisionVersionStatus.BUILDING));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecisionUpdateSuccess() {

        Decisions apiRequest = createApiRequest();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");
        apiRequest.getModel().setMd5("sdsdsd");

        decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        long nextVersion = decision.getNextVersion().getVersion();

        decision = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(nextVersion));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecisionUpdateFailed() {

        Decisions apiRequest = createApiRequest();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");
        apiRequest.getModel().setMd5("sdsdsd");

        decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        long currentVersion = decision.getCurrentVersion().getVersion();

        decision = decisionManager.failed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(currentVersion));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @Test
    public void deleteDecision() {

        Decisions apiRequest = createApiRequest();
        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decision = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        String versionId = decision.getCurrentVersion().getId();

        decisionManager.deleteDecision(decision.getCustomerId(), apiRequest);

        assertThat(decisionDAO.findById(decision.getId()), is(nullValue()));
        assertThat(decisionVersionDAO.findById(versionId), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void deleteVersion_versionDoesNotExist() {

        Decisions apiRequest = createApiRequest();

        assertThrows(DecisionLifecycleException.class, () -> {
            decisionManager.deleteVersion(customerIdResolver.getCustomerId(), apiRequest);
        });
    }

    @TestTransaction
    @Test
    public void deleteVersion() {

        Decisions apiRequest = createApiRequest();

        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionManager.deployed(customerIdResolver.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        apiRequest.getModel().setMd5("foososo");
        decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decision = decisionManager.deployed(customerIdResolver.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        assertThat(decision.getCurrentVersion().getVersion(), equalTo(2l));
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));

        Decisions deleteRequest = createApiRequestWithVersion(1l);
        DecisionVersion deletedVersion = decisionManager.deleteVersion(customerIdResolver.getCustomerId(), deleteRequest);
        assertThat(deletedVersion.getStatus(), equalTo(DecisionVersionStatus.DELETED));
    }

    @Test
    @TestTransaction
    public void deleteVersion_versionIsCurrentVersion() {

        Decisions apiRequest = createApiRequest();
        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decision = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decision.getCurrentVersion().getVersion());

        Decisions deleteRequest = createApiRequestWithVersion(decision.getCurrentVersion().getVersion());

        assertThrows(DecisionLifecycleException.class, () -> {
            decisionManager.deleteVersion(customerIdResolver.getCustomerId(), deleteRequest);
        });
    }

    @Test
    @TestTransaction
    public void deleteVersion_canDeleteAFailedVersion() {
        Decisions apiRequest = createApiRequest();
        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decision = decisionManager.failed(customerIdResolver.getCustomerId(), decision.getName(), decision.getNextVersion().getVersion());

        Decisions deleteRequest = createApiRequestWithVersion(decision.getCurrentVersion().getVersion());
        DecisionVersion decisionVersion = decisionManager.deleteVersion(customerIdResolver.getCustomerId(), deleteRequest);

        assertThat(decisionVersion.getStatus(), equalTo(DecisionVersionStatus.DELETED));
        assertThat(decisionVersion.getDecision().getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.DELETED));
    }

    @TestTransaction
    @Test
    public void deleteVersion_versionInvolvedInLifecycleOperation() {
        Decisions apiRequest = createApiRequest();
        Decision decision = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decisions deleteRequest = createApiRequestWithVersion(decision.getCurrentVersion().getVersion());

        assertThrows(DecisionLifecycleException.class, () -> {
            decisionManager.deleteVersion(customerIdResolver.getCustomerId(), deleteRequest);
        });
    }
}
