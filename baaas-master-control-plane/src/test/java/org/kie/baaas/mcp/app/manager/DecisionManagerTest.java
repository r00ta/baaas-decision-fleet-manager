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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
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
import org.kie.baaas.mcp.app.model.deployment.Deployment;
import org.kie.baaas.mcp.app.model.eventing.KafkaTopics;
import org.kie.baaas.mcp.app.resolvers.CustomerIdResolver;
import org.kie.baaas.mcp.app.storage.DMNStorageRequest;
import org.kie.baaas.mcp.app.storage.DecisionDMNStorage;
import org.kie.baaas.mcp.app.storage.s3.S3DMNStorage;
import org.mockito.Mockito;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

    @Inject
    DecisionDMNStorage dmnStorage;

    private DecisionRequest createApiRequest() {

        Model model = new Model();
        model.setDmn("foo");

        DecisionRequest decisions = new DecisionRequest();
        decisions.setDescription("The Best Decision Ever");
        decisions.setName("robs-first-decision");
        decisions.setModel(model);
        return decisions;
    }

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

    @TestTransaction
    @Test
    public void createNewVersion_withKafka() {
        DMNStorageRequest request = createStorageRequest();

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
        assertThat(decisionVersion.getDmnLocation(), equalTo(request.getProviderUrl()));
        assertThat(decisionVersion.getDmnMd5(), equalTo(request.getMd5Hash()));

    }

    @TestTransaction
    @Test
    public void createNewVersion_newDecision() {
        DMNStorageRequest request = createStorageRequest();
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
        assertThat(decisionVersion.getDmnMd5(), equalTo(request.getMd5Hash()));
        assertThat(decisionVersion.getDmnLocation(), equalTo((request.getProviderUrl())));

        ZonedDateTime zdt = ZonedDateTime.parse(decisionVersion.getSubmittedAt().toString(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        assertThat(zdt, is(notNullValue()));
    }

    private Deployment createDeployment() {
        Deployment deployment = new Deployment();
        deployment.setUrl("http://foo");
        deployment.setVersionName("my-version");
        deployment.setNamespace("my-namespace");
        deployment.setName("my-name");
        return deployment;
    }

    @TestTransaction
    @Test
    public void deployed_withFirstVersionOfDecision() {
        DMNStorageRequest request = createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        Deployment deployment = createDeployment();
        DecisionVersion deployed = decisionManager.deployed(decision.getCustomerId(), decision.getId(), decisionVersion.getVersion(), deployment);

        assertThat(deployed, is(notNullValue()));
        assertThat(deployed.getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(deployed.getPublishedAt(), is(notNullValue()));
        assertThat(deployed.getDeployment(), is(notNullValue()));
        assertThat(deployed.getDecision().getCurrentVersion().getVersion(), equalTo(deployed.getVersion()));
        assertThat(deployed.getDecision().getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void failed_withFirstVersionOfDecision() {
        DMNStorageRequest request = createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        DecisionVersion deployed = decisionManager.failed(decision.getCustomerId(), decision.getId(), decisionVersion.getVersion(), createDeployment());
        assertThat(deployed, is(notNullValue()));
        assertThat(deployed.getStatus(), equalTo(DecisionVersionStatus.FAILED));
        assertThat(deployed.getDmnMd5(), equalTo(request.getMd5Hash()));
        assertThat(deployed.getDmnLocation(), equalTo(request.getProviderUrl()));

        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.FAILED));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateFailsWhenStillCreating() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("Updated dmn!");

        DecisionLifecycleException thrown = assertThrows(DecisionLifecycleException.class, () -> decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest));

        assertThat(thrown.getMessage(), equalTo("A lifecycle operation is already in progress for Version '1' of Decision 'robs-first-decision'"));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecision() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());

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
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");

        DecisionVersion nextVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        nextVersion = decisionManager.deployed(decision.getCustomerId(), decision.getName(), nextVersion.getVersion(), createDeployment());
        assertThat(nextVersion.getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(nextVersion.getDeployment().getUrl(), is(notNullValue()));

        decision = nextVersion.getDecision();
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(nextVersion.getVersion()));
        assertThat(decision.getNextVersion(), is(nullValue()));

        decisionVersion = decisionVersionDAO.findById(decisionVersion.getId());
        assertThat(decisionVersion.getDeployment().getUrl(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_twoFailures() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();
        decisionManager.failed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");

        DecisionVersion failedVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        failedVersion = decisionManager.failed(decision.getCustomerId(), decision.getName(), failedVersion.getVersion(), createDeployment());
        assertThat(failedVersion.getStatus(), equalTo(DecisionVersionStatus.FAILED));

        decision = failedVersion.getDecision();
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.FAILED));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(failedVersion.getVersion()));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecisionUpdateFailed() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();
        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");

        DecisionVersion failedVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        failedVersion = decisionManager.failed(decision.getCustomerId(), decision.getName(), failedVersion.getVersion(), createDeployment());
        assertThat(failedVersion.getStatus(), equalTo(DecisionVersionStatus.FAILED));

        decision = failedVersion.getDecision();
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(decisionVersion.getVersion()));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @Test
    public void deleteDecision() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionVersion = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());
        decisionManager.deleteDecision(decision.getCustomerId(), decision.getName());

        assertThat(decisionDAO.findById(decisionVersion.getId()), is(nullValue()));
        assertThat(decisionVersionDAO.findById(decisionVersion.getId()), is(nullValue()));
    }

    @Test
    public void deleteDecision_decisionDoesNotExist() {
        assertThrows(NoSuchDecisionException.class, () -> {
            decisionManager.deleteDecision(customerIdResolver.getCustomerId(), "foo");
        });
    }

    @TestTransaction
    @Test
    public void deleteVersion_versionDoesNotExist() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        assertThrows(NoSuchDecisionVersionException.class, () -> decisionManager.deleteVersion(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion() + 1l));
    }

    @TestTransaction
    @Test
    public void deleteVersion() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();
        decisionManager.deployed(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest.getModel().setDmn("updated dmn!");

        decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionVersion = decisionManager.deployed(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());
        decision = decisionVersion.getDecision();

        assertThat(decision.getCurrentVersion().getVersion(), equalTo(2L));
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));

        DecisionVersion deletedVersion = decisionManager.deleteVersion(customerIdResolver.getCustomerId(), decision.getName(), 1l);
        assertThat(deletedVersion.getStatus(), equalTo(DecisionVersionStatus.DELETED));
    }

    @Test
    @TestTransaction
    public void deleteVersion_versionIsCurrentVersion() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        DecisionVersion deployed = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());
        assertThrows(DecisionLifecycleException.class, () -> decisionManager.deleteVersion(customerIdResolver.getCustomerId(), decision.getName(), deployed.getVersion()));
    }

    @Test
    @TestTransaction
    public void deleteVersion_canDeleteAFailedVersion() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionVersion = decisionManager.failed(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());

        decisionVersion = decisionManager.deleteVersion(customerIdResolver.getCustomerId(), decision.getName(), decisionVersion.getVersion());
        assertThat(decisionVersion.getStatus(), equalTo(DecisionVersionStatus.DELETED));
        assertThat(decisionVersion.getDecision().getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.DELETED));
    }

    @TestTransaction
    @Test
    public void listDecisionVersions_withDecisionId() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        List<DecisionVersion> versions = decisionManager.listDecisionVersions(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getId());
        assertThat(versions, hasSize(1));
        assertThat(versions.get(0).getId(), equalTo(decisionVersion.getId()));
    }

    @TestTransaction
    @Test
    public void getVersion_decisionDoesNotExist() {
        NoSuchDecisionException thrown = assertThrows(NoSuchDecisionException.class, () -> {
            decisionManager.getVersion(customerIdResolver.getCustomerId(), "foo", 1l);
        });

        assertThat(thrown.getMessage(), equalTo("Decision with id or name 'foo' does not exist for customer '1'"));
    }

    @TestTransaction
    @Test
    public void getVersion_versionDoesNotExist() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        assertThrows(NoSuchDecisionVersionException.class, () -> {
            decisionManager.getVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getId(), decisionVersion.getVersion() + 1l);
        });
    }

    @TestTransaction
    @Test
    public void getVersion_byDecisionId() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        DecisionVersion found = decisionManager.getVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getId(), decisionVersion.getVersion());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @TestTransaction
    @Test
    public void getVersion_byDecisionName() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        DecisionVersion found = decisionManager.getVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName(), decisionVersion.getVersion());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @TestTransaction
    @Test
    public void getCurrentVersion_byDecisionName() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        DecisionVersion found = decisionManager.getCurrentVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @TestTransaction
    @Test
    public void getCurrentVersion_byDecisionId() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        DecisionVersion found = decisionManager.getCurrentVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getId());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @Test
    @TestTransaction
    public void getBuildingVersion_byDecisionId() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        DecisionVersion found = decisionManager.getBuildingVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getId());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @Test
    @TestTransaction
    public void getBuildingVersion_byDecisionName() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        DecisionVersion found = decisionManager.getBuildingVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @Test
    @TestTransaction
    public void getBuildingVersion_noBuildingVersion() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionManager.deployed(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getId(), decisionVersion.getVersion(), createDeployment());

        assertThrows(NoSuchDecisionVersionException.class, () -> {
            decisionManager.getBuildingVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName());
        });
    }

    @Test
    public void getCurrentVersion_decisionDoesNotExist() {

        NoSuchDecisionException thrown = assertThrows(NoSuchDecisionException.class, () -> {
            decisionManager.getCurrentVersion(customerIdResolver.getCustomerId(), "foo");
        });

        assertThat(thrown.getMessage(), equalTo("Decision with id or name 'foo' does not exist for customer '1'"));
    }

    @TestTransaction
    @Test
    public void listDecisionVersions_withDecisionName() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        List<DecisionVersion> versions = decisionManager.listDecisionVersions(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName());
        assertThat(versions, hasSize(1));
        assertThat(versions.get(0).getId(), equalTo(decisionVersion.getId()));
    }

    @TestTransaction
    @Test
    public void listDecisionVersions_noSuchIdOrName() {

        NoSuchDecisionException thrown = assertThrows(NoSuchDecisionException.class, () -> {
            decisionManager.listDecisionVersions(customerIdResolver.getCustomerId(), "foo");
        });

        assertThat(thrown.getMessage(), equalTo("Decision with id or name 'foo' does not exist for customer '1'"));
    }

    @TestTransaction
    @Test
    public void listDecisions() {
        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionRequest apiRequest2 = createApiRequest();
        apiRequest2.setName("another-decision");

        DecisionRequest apiRequest3 = createApiRequest();
        apiRequest3.setName("yet-another-decision");

        Map<String, String> config = new HashMap<>();
        config.put("configKey", "configValue");
        apiRequest.setConfiguration(config);

        Map<String, String> tags = new HashMap<>();
        tags.put("tagKey", "tagValue");
        apiRequest2.setTags(tags);

        decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest2);
        decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest3);

        List<Decision> decisions = decisionManager.listDecisions(customerIdResolver.getCustomerId());
        assertThat(decisions, hasSize(3));

        Decision found = decisions.get(0);
        assertThat(found.getName(), equalTo(apiRequest2.getName()));
        assertThat(found.getCurrentVersion().getConfiguration(), is(anEmptyMap()));
        assertThat(found.getCurrentVersion().getTags().containsKey("tagKey"), is(true));

        found = decisions.get(1);
        assertThat(found.getName(), equalTo(apiRequest.getName()));
        assertThat(found.getCurrentVersion().getConfiguration().containsKey("configKey"), is(true));
        assertThat(found.getCurrentVersion().getTags(), is(anEmptyMap()));

        found = decisions.get(2);
        assertThat(found.getName(), equalTo(apiRequest3.getName()));
        assertThat(found.getCurrentVersion().getConfiguration(), is(anEmptyMap()));
        assertThat(found.getCurrentVersion().getTags(), is(anEmptyMap()));
    }

    @TestTransaction
    @Test
    public void newDecision_decisionDoesNotExist() {
        assertThrows(NoSuchDecisionException.class, () -> {
            decisionManager.setCurrentVersion(customerIdResolver.getCustomerId(), "foo", 1);
        });
    }

    @TestTransaction
    @Test
    public void newDecision_decisionVersionDoesNotExist() {
        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        assertThrows(NoSuchDecisionVersionException.class, () -> {
            decisionManager.setCurrentVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName(), decisionVersion.getVersion() + 1l);
        });
    }

    @Test
    @TestTransaction
    public void newDecision_decisionVersionNotInReadyState() {
        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        DecisionVersion deployed = decisionManager.deployed(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        assertThrows(DecisionLifecycleException.class, () -> decisionManager.setCurrentVersion(customerIdResolver.getCustomerId(), deployed.getDecision().getId(), deployed.getVersion()));
    }

    @TestTransaction
    @Test
    public void newDecision_lifecycleOperationAlreadyInProgress() {
        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        decisionManager.deployed(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest = createApiRequest();
        apiRequest.getModel().setDmn("Updated dmn!");

        decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionManager.deployed(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest = createApiRequest();
        apiRequest.getModel().setDmn("Updated dmn again!");

        decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        DecisionVersion firstVersion = decisionManager.getVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getId(), 1l);
        assertThat(firstVersion.getStatus(), equalTo(DecisionVersionStatus.READY));

        assertThrows(DecisionLifecycleException.class, () -> decisionManager.setCurrentVersion(customerIdResolver.getCustomerId(), firstVersion.getDecision().getId(), firstVersion.getVersion()));
    }

    @TestTransaction
    @Test
    public void newVersion() {
        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);

        decisionManager.deployed(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest = createApiRequest();
        apiRequest.getModel().setDmn("Updated dmn!");

        decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionManager.deployed(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest = createApiRequest();
        apiRequest.getModel().setDmn("Updated dmn again!");

        decisionVersion = decisionManager.createOrUpdateVersion(customerIdResolver.getCustomerId(), apiRequest);
        decisionVersion = decisionManager.deployed(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        DecisionVersion firstVersion = decisionManager.getVersion(customerIdResolver.getCustomerId(), decisionVersion.getDecision().getId(), 1l);
        assertThat(firstVersion.getStatus(), equalTo(DecisionVersionStatus.READY));

        DecisionVersion newVersion = decisionManager.setCurrentVersion(customerIdResolver.getCustomerId(), firstVersion.getDecision().getId(), firstVersion.getVersion());
        assertThat(newVersion.getStatus(), equalTo(DecisionVersionStatus.BUILDING));
        assertThat(newVersion.getDecision().getNextVersion().getVersion(), equalTo(newVersion.getVersion()));
        assertThat(newVersion.getDecision().getCurrentVersion().getVersion(), equalTo(decisionVersion.getVersion()));
        assertThat(newVersion.getDecision().getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
    }
}
